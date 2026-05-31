package com.cubiquitous.tracura.ui.view.businesshead

import android.content.Context
import android.R.attr.onClick
import android.R.attr.shape
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.DepartmentBudget
import com.cubiquitous.tracura.model.DepartmentDraft
import com.cubiquitous.tracura.model.ContractorMode
import com.cubiquitous.tracura.model.LineItem
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.PhaseDraft
import com.cubiquitous.tracura.model.PhaseWithDepartments
import com.cubiquitous.tracura.viewmodel.BusinessHeadViewModel
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.model.ProjectDraft
import com.cubiquitous.tracura.ui.common.DepartmentLineItemsSection
import com.cubiquitous.tracura.ui.common.AddDepartmentModalSheet
import com.cubiquitous.tracura.ui.common.AddLineItemModalSheet
import com.cubiquitous.tracura.model.ProjectTemplate
import com.cubiquitous.tracura.model.ProjectTemplates
import com.google.firebase.Timestamp
import com.google.gson.Gson
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import com.cubiquitous.tracura.ui.common.ApproverAssignmentSheet
import com.cubiquitous.tracura.ui.common.UserAssignmentSheet
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.cubiquitous.tracura.R

private const val NEW_PROJECT_AUTOSAVE_PREFS = "new_project_autosave_prefs"
private const val NEW_PROJECT_AUTOSAVE_KEY_PREFIX = "new_project_autosave"

data class CountryCode(
    val name: String,
    val dialCode: String,
    val flag: String,
    val minDigits: Int,
    val maxDigits: Int
)

val allCountryCodes = listOf(
    CountryCode("India", "+91", "🇮🇳", 10, 10),
    CountryCode("United States", "+1", "🇺🇸", 10, 10),
    CountryCode("United Kingdom", "+44", "🇬🇧", 10, 10),
    CountryCode("UAE", "+971", "🇦🇪", 9, 9),
    CountryCode("Saudi Arabia", "+966", "🇸🇦", 9, 9),
    CountryCode("Australia", "+61", "🇦🇺", 9, 9),
    CountryCode("Singapore", "+65", "🇸🇬", 8, 8),
    CountryCode("Canada", "+1", "🇨🇦", 10, 10),
    CountryCode("Germany", "+49", "🇩🇪", 10, 12),
    CountryCode("France", "+33", "🇫🇷", 9, 9),
    CountryCode("China", "+86", "🇨🇳", 11, 11),
    CountryCode("Japan", "+81", "🇯🇵", 10, 11),
    CountryCode("Bangladesh", "+880", "🇧🇩", 10, 10),
    CountryCode("Pakistan", "+92", "🇵🇰", 10, 10),
    CountryCode("Sri Lanka", "+94", "🇱🇰", 9, 9),
    CountryCode("Nepal", "+977", "🇳🇵", 10, 10),
    CountryCode("South Africa", "+27", "🇿🇦", 9, 9),
    CountryCode("Brazil", "+55", "🇧🇷", 10, 11),
    CountryCode("Indonesia", "+62", "🇮🇩", 9, 12),
    CountryCode("Malaysia", "+60", "🇲🇾", 9, 10),
    CountryCode("Philippines", "+63", "🇵🇭", 10, 10),
    CountryCode("Thailand", "+66", "🇹🇭", 9, 9),
    CountryCode("Qatar", "+974", "🇶🇦", 8, 8),
    CountryCode("Kuwait", "+965", "🇰🇼", 8, 8),
    CountryCode("Bahrain", "+973", "🇧🇭", 8, 8),
    CountryCode("Oman", "+968", "🇴🇲", 8, 8),
)

// Safe serializable phase draft – uses Long? for dates to avoid Gson's unreliable Date handling
data class AutosavePhaseDraft(
    val phaseName: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val departments: List<DepartmentDraft> = emptyList(),
    val categories: List<String> = emptyList()
)

data class LocalProjectAutosaveDraft(
    val projectName: String = "",
    val description: String = "",
    val client: String = "",
    val clientPrimaryNumber: String = "",
    val clientSecondaryNumber: String = "",
    val primaryCountryDialCode: String = "+91",
    val secondaryCountryDialCode: String = "+91",
    val location: String = "",
    val currency: String = "INR",
    val plannedStartDate: Long? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val handoverDate: Long? = null,
    val maintenanceDate: Long? = null,
    val selectedMaintenanceOption: String? = null,
    val selectedManagerUid: String? = null,
    val selectedApproverUid: String? = null,
    val selectedTeamMemberUids: List<String> = emptyList(),
    val phases: List<AutosavePhaseDraft> = emptyList(),
    val projectCategories: List<String> = emptyList(),
    val categoryName: String = "",
    val initialUserAssignments: Map<String, String> = emptyMap(),
    val initialApproverAssignments: Map<String, String> = emptyMap(),
    val selectedTemplateId: String? = null,
    val lastSaved: Long = System.currentTimeMillis()
)

/**
 * Returns the template description based on the project type name
 */
fun getTemplateDescription(projectType: String?): String {
    if (projectType == null) {
        return "Standard template for residential building construction with common phases and departments"
    }

    return when (projectType) {
        "Residential Building" -> "Standard template for residential building construction with common phases and departments"
        "Renovation" -> "Template for building renovation and remodeling projects with comprehensive phases and departments"
        "Commercial Office" -> "Template for commercial office space construction and fit-out with standard phases and departments"
        "Road Infrastructure" -> "Template for road construction and infrastructure projects with essential phases and departments"
        "Residential Interior Design" -> "Comprehensive interior design template for residential spaces including woodwork, false ceiling, and finishing"
        "Commercial Office Interior" -> "Complete interior design solution for modern commercial office spaces including workstations, meeting rooms, and common areas"
        "Restaurant Interior Design", "Restaurant & Cafe Interior" -> "Complete interior design for restaurant/cafe including dining area, kitchen layout, and ambiance creation"
        "Luxury Villa Interior" -> "Premium interior design for luxury villas including high-end finishes, custom furniture, and smart home integration"
        "Ad Film Production" -> "Template for TV Commercial/Digital Ad Film production including pre-production, shoot, and post-production"
        "Corporate Video Production" -> "Professional corporate video production including company profile, product showcase, and testimonial videos"
        "Event Photography & Videography" -> "Complete event coverage package including photography, videography, and live streaming for corporate events, weddings, and conferences"
        "Social Media Content Package" -> "Complete social media content creation package including photos, videos, reels, and stories for Instagram, Facebook, and YouTube"
        else -> "Standard template with common phases and departments"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    onNavigateBack: () -> Unit,
    onProjectCreated: () -> Unit,
    onNavigateToReview: (String, String, String, String?, String?, String, String, Date?, Date?, Date?, List<PhaseDraft>, User?, List<User>, Map<String, String>, Map<String, String>) -> Unit = { _: String, _: String, _: String, _: String?, _: String?, _: String, _: String, _: Date?, _: Date?, _: Date?, _: List<PhaseDraft>, _: User?, _: List<User>, _: Map<String, String>, _: Map<String, String> -> },
    onNavigateToDraftProjects: () -> Unit = {},
    selectedDraftId: String? = null,
    projectIdForResubmission: String? = null, // For resubmitting REVIEW REJECTED projects
    selectedTemplate: ProjectTemplate? = null, // For loading from template
    viewModel: BusinessHeadViewModel = hiltViewModel()
) {
    var projectName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val descriptionWordCount = remember(description) {
        description.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    }
    val maxDescriptionWords = 150
    var client by remember { mutableStateOf("") }
    var clientPrimaryNumber by remember { mutableStateOf("") }
    var clientSecondaryNumber by remember { mutableStateOf("") }
    val indiaCountryCode = allCountryCodes.first()
    var primaryCountryCode by remember { mutableStateOf(indiaCountryCode) }
    var secondaryCountryCode by remember { mutableStateOf(indiaCountryCode) }
    var primaryCountryCodeExpanded by remember { mutableStateOf(false) }
    var secondaryCountryCodeExpanded by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var plannedStartDate by remember { mutableStateOf<Date?>(null) }
    var handoverDate by remember { mutableStateOf<Date?>(null) }
    var maintenanceDate by remember { mutableStateOf<Date?>(null) }
    var showPlannedStartDatePicker by remember { mutableStateOf(false) }
    var showHandoverDatePicker by remember { mutableStateOf(false) }
    var showMaintenanceDatePicker by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Auto-select phase dates toggle
    var autoSelectPhaseDates by remember { mutableStateOf(true) }
    // Date picker states
    val plannedStartDatePickerState = rememberDatePickerState()
    val handoverDatePickerState = rememberDatePickerState()
    val maintenanceDatePickerState = rememberDatePickerState()
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()
    var isFocused by remember { mutableStateOf(false) }

    // Date validation errors
    var handoverDateError by remember { mutableStateOf<String?>(null) }
    var maintenanceDateError by remember { mutableStateOf<String?>(null) }

    // Maintenance Date dropdown state
    var showMaintenanceDateDropdown by remember { mutableStateOf(false) }
    var selectedMaintenanceOption by remember { mutableStateOf<String?>(null) } // "1 Month", "2 Months", etc., or "Custom Date"

    // Toast context
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val autosavePrefs = remember(context) {
        context.getSharedPreferences(NEW_PROJECT_AUTOSAVE_PREFS, Context.MODE_PRIVATE)
    }
    val autosaveKey = remember {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        "${NEW_PROJECT_AUTOSAVE_KEY_PREFIX}_$uid"
    }
    var hasAttemptedLocalRestore by remember { mutableStateOf(false) }
    var pendingLocalManagerUid by remember { mutableStateOf<String?>(null) }
    var pendingLocalApproverUid by remember { mutableStateOf<String?>(null) }
    var pendingLocalTeamMemberUids by remember { mutableStateOf<List<String>>(emptyList()) }

    fun clearLocalAutosaveDraft() {
        autosavePrefs.edit().remove(autosaveKey).apply()
    }

    // Firestore draft state
    val firestoreDrafts by viewModel.firestoreDrafts.collectAsState()
    val isSavingDraft by viewModel.isSavingDraft.collectAsState()
    var isDraftLoaded by remember { mutableStateOf(false) }
    // Track the last loaded draft ID to prevent reloading the same draft
    var lastLoadedDraftId by remember { mutableStateOf<String?>(null) }
    // Flag to prevent draft reloading after successful project creation
    var justSubmittedProject by remember { mutableStateOf(false) }

    // Manager Selection
    var selectedManager by remember { mutableStateOf<User?>(null) }
    var managerSearchQuery by remember { mutableStateOf("") }
    var showManagerDropdown by remember { mutableStateOf(false) }

    // Hoisted initial assignments for DECLINED project pre-fill
    var initialUserAssignments by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var initialApproverAssignments by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var finalUserAssignments by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var finalApproverAssignments by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Team Assignment
    var selectedApprover by remember { mutableStateOf<User?>(null) }
    var selectedTeamMembers by remember { mutableStateOf<List<User>>(emptyList()) }
    var showApproverSearch by remember { mutableStateOf(false) }
    var showTeamMemberSearch by remember { mutableStateOf(false) }
    var approverSearchQuery by remember { mutableStateOf("") }
    var teamMemberSearchQuery by remember { mutableStateOf("") }

    // Safe team member operations
    fun addTeamMember(user: User) {
        try {
            android.util.Log.d("NewProjectScreen", "Adding team member: ${user.name} (${user.uid})")

            // Validate user object
            if (user.uid.isNullOrEmpty()) {
                android.util.Log.e("NewProjectScreen", "User UID is null or empty")
                Toast.makeText(context, "Invalid user data", Toast.LENGTH_SHORT).show()
                return
            }

            // Check if user is already selected
            val isAlreadySelected = selectedTeamMembers.any { selectedUser ->
                selectedUser.uid == user.uid
            }

            if (!isAlreadySelected) {
                selectedTeamMembers = selectedTeamMembers + user
                android.util.Log.d(
                    "NewProjectScreen",
                    "Successfully added team member. New count: ${selectedTeamMembers.size}"
                )
            } else {
                android.util.Log.d("NewProjectScreen", "User already selected: ${user.name}")
                Toast.makeText(context, "${user.name} is already selected", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            android.util.Log.e("NewProjectScreen", "Error adding team member: ${e.message}", e)
            Toast.makeText(context, "Error adding team member: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun removeTeamMember(user: User) {
        try {
            android.util.Log.d(
                "NewProjectScreen", "Removing team member: ${user.name} (${user.uid})"
            )
            selectedTeamMembers = selectedTeamMembers.filter { it.uid != user.uid }
            android.util.Log.d(
                "NewProjectScreen",
                "Successfully removed team member. New count: ${selectedTeamMembers.size}"
            )
        } catch (e: Exception) {
            android.util.Log.e("NewProjectScreen", "Error removing team member: ${e.message}", e)
            Toast.makeText(context, "Error removing team member: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    // Department Budgets
    var departmentName by remember { mutableStateOf("") }
    var departmentBudgetAmount by remember { mutableStateOf("") }

    // Categories
    var categoryName by remember { mutableStateOf("") }
    var projectCategories by remember { mutableStateOf<List<String>>(emptyList()) }

    // No predefined departments - users can type any department name

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val availableApprovers by viewModel.availableApprovers.collectAsState()
    val availableUsers by viewModel.availableUsers.collectAsState()
    val availableManagers by viewModel.availableManagers.collectAsState()
    val reviewData by viewModel.reviewData.collectAsState()
    val allExistingPhases by viewModel.allExistingPhases.collectAsState()
    val businessType by viewModel.businessType.collectAsState()
    val departmentUserCounts by viewModel.departmentUserCounts.collectAsState()
    val departmentApproverCounts by viewModel.departmentApproverCounts.collectAsState()
    val phases = remember { mutableStateListOf(PhaseDraft()) }
    var activePhaseIndexForStart by remember { mutableStateOf<Int?>(null) }
    var activePhaseIndexForEnd by remember { mutableStateOf<Int?>(null) }

    // Track which phases are expanded
    var expandedPhases by remember { mutableStateOf(setOf<Int>(0)) } // First phase expanded by default

    // Track if template has been loaded to prevent overwriting
    var isTemplateLoaded by remember { mutableStateOf(false) }

    // Internal template selection state (for dropdown)
    var selectedTemplateInternal by remember { mutableStateOf<ProjectTemplate?>(null) }
    var templateDropdownExpanded by remember { mutableStateOf(false) }

    // Load business type on screen initialization
    LaunchedEffect(Unit) {
        viewModel.loadBusinessType()
    }
    // Fetch the customer data from customer collection and store counts in ViewModel
    // Only two variables are stored: departmentApproverCounts and departmentUserCounts
    LaunchedEffect(Unit) {
        viewModel.loadCustomerDepartments()
    }
    // Load users (includes managers) so the manager dropdown is populated
    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }
    // Load Firestore drafts for banner and draft list
    LaunchedEffect(Unit) {
        viewModel.loadFirestoreDrafts()
    }

    // Filter templates based on business type
    val availableTemplates = remember(ProjectTemplates.templates, businessType) {
        if (businessType != null) {
            val filtered = ProjectTemplates.templates.filter {
                it.businessType == businessType
            }
            android.util.Log.d(
                "NewProjectScreen",
                "✅ Filtered ${filtered.size} templates for business type: $businessType"
            )
            filtered.forEach { template ->
                android.util.Log.d("NewProjectScreen", "  - ${template.name} (${template.id})")
            }
            filtered
        } else {
            // If businessType is null, show all templates as fallback
            android.util.Log.d(
                "NewProjectScreen", "⚠️ Business type is null, showing all templates"
            )
            ProjectTemplates.templates
        }
    }

    // Use internal template if available, otherwise use parameter template
    val activeTemplate = selectedTemplateInternal ?: selectedTemplate

    // Restore locally autosaved draft once for a fresh new-project form.
    LaunchedEffect(selectedDraftId, projectIdForResubmission, selectedTemplate?.id, hasAttemptedLocalRestore) {
        if (hasAttemptedLocalRestore) return@LaunchedEffect

        if (selectedDraftId != null || projectIdForResubmission != null || selectedTemplate != null) {
            return@LaunchedEffect
        }

        val savedJson = autosavePrefs.getString(autosaveKey, null)
        if (savedJson.isNullOrBlank()) {
            hasAttemptedLocalRestore = true
            return@LaunchedEffect
        }

        runCatching {
            gson.fromJson(savedJson, LocalProjectAutosaveDraft::class.java)
        }.onSuccess { localDraft ->
            if (localDraft != null) {
                projectName = localDraft.projectName
                description = localDraft.description
                client = localDraft.client
                clientPrimaryNumber = localDraft.clientPrimaryNumber
                clientSecondaryNumber = localDraft.clientSecondaryNumber
                primaryCountryCode = allCountryCodes.find { it.dialCode == localDraft.primaryCountryDialCode } ?: indiaCountryCode
                secondaryCountryCode = allCountryCodes.find { it.dialCode == localDraft.secondaryCountryDialCode } ?: indiaCountryCode
                location = localDraft.location
                currency = localDraft.currency.ifEmpty { "INR" }
                plannedStartDate = localDraft.plannedStartDate?.let { Date(it) }
                startDate = localDraft.startDate?.let { Date(it) }
                endDate = localDraft.endDate?.let { Date(it) }
                handoverDate = localDraft.handoverDate?.let { Date(it) }
                maintenanceDate = localDraft.maintenanceDate?.let { Date(it) }
                selectedMaintenanceOption = localDraft.selectedMaintenanceOption
                categoryName = localDraft.categoryName
                projectCategories = localDraft.projectCategories
                initialUserAssignments = localDraft.initialUserAssignments
                initialApproverAssignments = localDraft.initialApproverAssignments
                finalUserAssignments = localDraft.initialUserAssignments
                finalApproverAssignments = localDraft.initialApproverAssignments

                plannedStartDatePickerState.selectedDateMillis = localDraft.plannedStartDate
                startDatePickerState.selectedDateMillis = localDraft.startDate
                endDatePickerState.selectedDateMillis = localDraft.endDate
                handoverDatePickerState.selectedDateMillis = localDraft.handoverDate
                maintenanceDatePickerState.selectedDateMillis = localDraft.maintenanceDate

                phases.clear()
                if (localDraft.phases.isNotEmpty()) {
                    localDraft.phases.forEach { autoPhase ->
                        phases.add(
                            PhaseDraft(
                                phaseName = autoPhase.phaseName,
                                start = autoPhase.startDate?.let { Date(it) },
                                end = autoPhase.endDate?.let { Date(it) },
                                departments = autoPhase.departments.map { dept ->
                                    dept.copy(lineItems = dept.lineItems.toMutableList())
                                }.toMutableList(),
                                categories = autoPhase.categories.toMutableList()
                            )
                        )
                    }
                } else {
                    phases.add(PhaseDraft())
                }

                pendingLocalManagerUid = localDraft.selectedManagerUid
                pendingLocalApproverUid = localDraft.selectedApproverUid
                pendingLocalTeamMemberUids = localDraft.selectedTeamMemberUids

                localDraft.selectedTemplateId?.let { savedTemplateId ->
                    val savedTemplate = availableTemplates.find { it.id == savedTemplateId }
                    if (savedTemplate != null) {
                        selectedTemplateInternal = savedTemplate
                    }
                }

                isDraftLoaded = true
                Toast.makeText(context, "Restored unsaved project draft", Toast.LENGTH_SHORT).show()
            }
        }.onFailure {
            clearLocalAutosaveDraft()
        }

        hasAttemptedLocalRestore = true
    }

    fun createLocalAutosaveSnapshot(): LocalProjectAutosaveDraft {
        return LocalProjectAutosaveDraft(
            projectName = projectName,
            description = description,
            client = client,
            clientPrimaryNumber = clientPrimaryNumber,
            clientSecondaryNumber = clientSecondaryNumber,
            primaryCountryDialCode = primaryCountryCode.dialCode,
            secondaryCountryDialCode = secondaryCountryCode.dialCode,
            location = location,
            currency = currency,
            plannedStartDate = plannedStartDate?.time,
            startDate = startDate?.time,
            endDate = endDate?.time,
            handoverDate = handoverDate?.time,
            maintenanceDate = maintenanceDate?.time,
            selectedMaintenanceOption = selectedMaintenanceOption,
            selectedManagerUid = selectedManager?.uid,
            selectedApproverUid = selectedApprover?.uid,
            selectedTeamMemberUids = selectedTeamMembers.mapNotNull { it.uid },
            phases = phases.map { phase ->
                AutosavePhaseDraft(
                    phaseName = phase.phaseName,
                    startDate = phase.start?.time,
                    endDate = phase.end?.time,
                    departments = phase.departments.map { dept ->
                        dept.copy(lineItems = dept.lineItems.toMutableList())
                    },
                    categories = phase.categories.toList()
                )
            },
            projectCategories = projectCategories,
            categoryName = categoryName,
            initialUserAssignments = finalUserAssignments,
            initialApproverAssignments = finalApproverAssignments,
            selectedTemplateId = selectedTemplateInternal?.id,
            lastSaved = System.currentTimeMillis()
        )
    }

    fun persistLocalAutosaveSnapshot() {
        val snapshot = createLocalAutosaveSnapshot()
        val hasPhaseData = snapshot.phases.any { phase ->
            phase.phaseName.isNotBlank() ||
                phase.startDate != null ||
                phase.endDate != null ||
                phase.categories.isNotEmpty() ||
                phase.departments.any { department ->
                    department.departmentName.isNotBlank() ||
                        department.contractorAmount > 0.0 ||
                        department.lineItems.any { lineItem ->
                            lineItem.itemType.isNotBlank() ||
                                lineItem.item.isNotBlank() ||
                                lineItem.spec.isNotBlank() ||
                                lineItem.quantity > 0.0 ||
                                lineItem.unitPrice > 0.0 ||
                                lineItem.uom.isNotBlank()
                        }
                }
        }

        val hasAnyData = snapshot.projectName.isNotBlank() ||
            snapshot.description.isNotBlank() ||
            snapshot.client.isNotBlank() ||
            snapshot.location.isNotBlank() ||
            snapshot.currency != "INR" ||
            snapshot.plannedStartDate != null ||
            snapshot.startDate != null ||
            snapshot.endDate != null ||
            snapshot.handoverDate != null ||
            snapshot.maintenanceDate != null ||
            snapshot.selectedMaintenanceOption != null ||
            snapshot.selectedManagerUid != null ||
            snapshot.selectedApproverUid != null ||
            snapshot.selectedTeamMemberUids.isNotEmpty() ||
            hasPhaseData ||
            snapshot.projectCategories.isNotEmpty() ||
            snapshot.categoryName.isNotBlank() ||
            snapshot.initialUserAssignments.isNotEmpty() ||
            snapshot.initialApproverAssignments.isNotEmpty()

        if (!hasAnyData) {
            clearLocalAutosaveDraft()
            return
        }

        autosavePrefs.edit().putString(autosaveKey, gson.toJson(snapshot)).apply()
    }

    // Restore selected objects after local draft restore when user lists are available.
    LaunchedEffect(availableManagers, availableApprovers, availableUsers, pendingLocalManagerUid, pendingLocalApproverUid, pendingLocalTeamMemberUids) {
        pendingLocalManagerUid?.let { managerPhone ->
            val manager = availableManagers.find { it.phone == managerPhone || (it.uid.isNotEmpty() && it.uid == managerPhone) }
            if (manager != null) {
                selectedManager = manager
                pendingLocalManagerUid = null
            }
        }

        pendingLocalApproverUid?.let { approverPhone ->
            val approver = availableApprovers.find { it.phone == approverPhone || (it.uid.isNotEmpty() && it.uid == approverPhone) }
            if (approver != null) {
                selectedApprover = approver
                pendingLocalApproverUid = null
            }
        }

        if (pendingLocalTeamMemberUids.isNotEmpty()) {
            val restoredMembers = pendingLocalTeamMemberUids.mapNotNull { phone ->
                availableUsers.find { it.phone == phone || (it.uid.isNotEmpty() && it.uid == phone) }
            }
            selectedTeamMembers = restoredMembers
            pendingLocalTeamMemberUids = emptyList()
        }
    }

    LaunchedEffect(availableManagers, selectedManager) {
        if (selectedManager == null && availableManagers.isNotEmpty()) {
            selectedManager = availableManagers.first()
        }
    }

    // Load template data if provided (moved after phases declaration)
    // This LaunchedEffect should run FIRST and with highest priority
    // Only load template if form is empty to prevent overwriting user-filled data
    LaunchedEffect(activeTemplate?.id) {
        if (activeTemplate != null && !isTemplateLoaded) {
            // Check if form has user-filled data - only load template if form is empty
            val hasUserData = projectName.isNotBlank() || 
                             description.isNotBlank() || 
                             client.isNotBlank() || 
                             location.isNotBlank() ||
                             phases.any { phase -> 
                                 phase.phaseName.isNotBlank() || 
                                 phase.departments.isNotEmpty() ||
                                 phase.start != null ||
                                 phase.end != null
                             }
            
            if (!hasUserData) {
                android.util.Log.d("NewProjectScreen", "📥 Loading template: ${activeTemplate.name} (form is empty)")
                android.util.Log.d(
                    "NewProjectScreen", "📥 Template has ${activeTemplate.phases.size} phases"
                )

                // Description remains empty for user to fill (project name and client name also remain empty)
                // description = "" // Already empty, user can type their own description

                // Clear existing phases and pre-fill from template
                phases.clear()
                activeTemplate.phases.forEachIndexed { index, phase ->
                    android.util.Log.d(
                        "NewProjectScreen",
                        "  Phase ${index + 1}: ${phase.phaseName}, Departments: ${phase.departments.size}"
                    )
                    phases.add(
                        PhaseDraft(
                            phaseName = phase.phaseName,
                            start = phase.start,
                            end = phase.end,
                            departments = phase.departments.map { dept ->
                                DepartmentDraft(
                                    departmentName = dept.departmentName,
                                    contractorMode = dept.contractorMode,
                                    lineItems = dept.lineItems.toMutableList()
                                )
                            }.toMutableList(),
                            categories = phase.categories.toMutableList()
                        )
                    )
                }

                // Expand first phase by default
                expandedPhases = setOf(0)

                // Mark template as loaded BEFORE other effects can interfere
                isTemplateLoaded = true
                android.util.Log.d("NewProjectScreen", "✅ Loaded template with ${phases.size} phases")
            } else {
                android.util.Log.d("NewProjectScreen", "⏸️ Skipping template load - form has user-filled data")
                // Don't mark as loaded so it can be loaded later if form is cleared
            }
        } else if (activeTemplate == null) {
            // Reset flag when template is cleared
            isTemplateLoaded = false
        }
    }

    // For resubmitting REVIEW REJECTED projects
    val editingProject by viewModel.editingProject.collectAsState()
    var isProjectLoaded by remember { mutableStateOf(false) }

    // Load project data if projectIdForResubmission is provided
    LaunchedEffect(projectIdForResubmission) {
        if (projectIdForResubmission != null && projectIdForResubmission.isNotBlank() && !isProjectLoaded) {
            android.util.Log.d(
                "NewProjectScreen", "🔄 Loading project for resubmission: $projectIdForResubmission"
            )
            viewModel.loadProjectForEdit(projectIdForResubmission)
            isProjectLoaded = true
        }
    }

    // Track if project data has been loaded to prevent re-loading
    var isProjectDataLoaded by remember { mutableStateOf(false) }

    // Populate form fields when project is loaded
    LaunchedEffect(editingProject, projectIdForResubmission, availableApprovers, availableUsers, availableManagers) {
        editingProject?.let { project ->
            if (projectIdForResubmission != null && project.id == projectIdForResubmission && !isProjectDataLoaded) {
                android.util.Log.d(
                    "NewProjectScreen", "📥 Populating form with project data: ${project.name}"
                )

                // Populate basic fields
                projectName = project.name
                description = project.description
                client = project.client
                clientPrimaryNumber = project.clientPrimaryNumber ?: ""
                clientSecondaryNumber = project.clientSecondaryNumber ?: ""
                location = project.location
                currency = project.currency.ifEmpty { "INR" }

                // Parse dates — prefer plannedDate (primary field), fall back to startDate (legacy)
                val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                val startDateStr = project.plannedDate ?: project.startDate
                startDateStr?.let { start ->
                    try {
                        plannedStartDate = inputFormat.parse(start)
                        plannedStartDate?.let {
                            plannedStartDatePickerState.selectedDateMillis = it.time
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "NewProjectScreen", "Error parsing start date: ${e.message}"
                        )
                    }
                }

                project.handoverDate?.let { handover ->
                    try {
                        handoverDate = inputFormat.parse(handover)
                        handoverDate?.let {
                            handoverDatePickerState.selectedDateMillis = it.time
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "NewProjectScreen", "Error parsing handover date: ${e.message}"
                        )
                    }
                }

                project.maintenanceDate?.let { maintenance ->
                    try {
                        maintenanceDate = inputFormat.parse(maintenance)
                        maintenanceDate?.let {
                            maintenanceDatePickerState.selectedDateMillis = it.time
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "NewProjectScreen", "Error parsing maintenance date: ${e.message}"
                        )
                    }
                }

                // Load Account Manager (managerId → selectedManager)
                val managerId = project.managerId
                if (managerId != null && availableManagers.isNotEmpty()) {
                    val manager = availableManagers.find { it.phone == managerId }
                    if (manager != null) {
                        selectedManager = manager
                        android.util.Log.d("NewProjectScreen", "✅ Pre-filled Account Manager: ${manager.name}")
                    }
                }

                // Load team members (wait for available users)
                val teamMemberPhones = project.teamMembers
                if (availableUsers.isNotEmpty()) {
                    selectedTeamMembers = availableUsers.filter { it.phone in teamMemberPhones }
                }

                // Pre-fill department user/approver assignments from project
                if (project.departmentUserAssignments.isNotEmpty()) {
                    initialUserAssignments = project.departmentUserAssignments
                    finalUserAssignments = project.departmentUserAssignments
                    android.util.Log.d("NewProjectScreen", "✅ Pre-filled user assignments: ${project.departmentUserAssignments.size} departments")
                }
                if (project.departmentApproverAssignments.isNotEmpty()) {
                    initialApproverAssignments = project.departmentApproverAssignments
                    finalApproverAssignments = project.departmentApproverAssignments
                    android.util.Log.d("NewProjectScreen", "✅ Pre-filled approver assignments: ${project.departmentApproverAssignments.size} departments")
                }

                // Load phases with departments from repository (fetches from subcollection)
                scope.launch {
                    try {
                        val phasesWithDepartments =
                            viewModel.getPhasesWithDepartmentsForProject(projectIdForResubmission)
                        android.util.Log.d(
                            "NewProjectScreen",
                            "📥 Loaded ${phasesWithDepartments.size} phases with departments for resubmission"
                        )

                        // Convert PhaseWithDepartments to PhaseDraft
                        // Don't clear if template is loaded
                        if (!isTemplateLoaded) {
                            phases.clear()
                        }
                        if (phasesWithDepartments.isNotEmpty()) {
                            phasesWithDepartments.forEach { phaseWithDepts ->
                                val phase = phaseWithDepts.phase
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val startDate = phase.startDate?.let { dateFormat.parse(it) }
                                val endDate = phase.endDate?.let { dateFormat.parse(it) }

                                // Convert Department objects to DepartmentDraft with line items
                                val departments = phaseWithDepts.departments.map { dept ->
                                    // Convert DepartmentLineItemData to LineItem
                                    val lineItems = dept.lineItems.map { lineItemData ->
                                        LineItem(
                                            itemType = lineItemData.itemType,
                                            item = lineItemData.item,
                                            spec = lineItemData.spec,
                                            quantity = lineItemData.quantity,
                                            unitPrice = lineItemData.unitPrice,
                                            uom = lineItemData.uom
                                        )
                                    }.toMutableList()

                                    // If no line items but there's a total budget, create a single line item
                                    if (lineItems.isEmpty() && dept.totalBudget > 0) {
                                        lineItems.add(
                                            LineItem(
                                                itemType = "",
                                                item = "",
                                                spec = "",
                                                quantity = 1.0,
                                                unitPrice = dept.totalBudget,
                                                uom = ""
                                            )
                                        )
                                    }

                                    DepartmentDraft(
                                        departmentName = dept.name,
                                        contractorMode = dept.contractorModeEnumValue,
                                        lineItems = lineItems,
                                        contractorAmount = dept.contractorAmount
                                    )
                                }.toMutableList()

                                // If no departments from subcollection, fallback to phase.departments Map (backward compatibility)
                                if (departments.isEmpty() && phase.departments.isNotEmpty()) {
                                    android.util.Log.d(
                                        "NewProjectScreen",
                                        "⚠️ No departments from subcollection, using phase.departments Map for phase: ${phase.phaseName}"
                                    )
                                    phase.departments.forEach { (deptName, budget) ->
                                        val draft = DepartmentDraft(
                                            departmentName = deptName,
                                            contractorMode = ContractorMode.LABOUR_ONLY,
                                            lineItems = mutableListOf()
                                        )
                                        // Create a line item with the budget amount to maintain totalBudget
                                        if (budget > 0) {
                                            draft.lineItems.add(
                                                LineItem(
                                                    itemType = "",
                                                    item = "",
                                                    spec = "",
                                                    quantity = 1.0,
                                                    unitPrice = budget,
                                                    uom = ""
                                                )
                                            )
                                        }
                                        departments.add(draft)
                                    }
                                }

                                phases.add(
                                    PhaseDraft(
                                        phaseName = phase.phaseName,
                                        start = startDate,
                                        end = endDate,
                                        departments = departments,
                                        categories = phase.categories.toMutableList()
                                    )
                                )

                                android.util.Log.d(
                                    "NewProjectScreen",
                                    "✅ Phase '${phase.phaseName}': ${departments.size} departments, total budget: ${departments.sumOf { it.totalBudget }}"
                                )
                            }
                            android.util.Log.d(
                                "NewProjectScreen",
                                "✅ Converted ${phases.size} phases to PhaseDraft with real budget values"
                            )
                        } else {
                            // If no phases, add at least one empty phase
                            phases.add(PhaseDraft())
                        }

                        isProjectDataLoaded = true
                        android.util.Log.d(
                            "NewProjectScreen",
                            "✅ Form populated with project data including real budget and phase values"
                        )
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "NewProjectScreen",
                            "❌ Error loading phases with departments: ${e.message}",
                            e
                        )
                        // Add at least one empty phase if loading fails
                        if (phases.isEmpty()) {
                            phases.add(PhaseDraft())
                        }
                        isProjectDataLoaded = true
                    }
                }
            }
        }
    }

    // Field error tracking for auto-scroll
    var fieldError by remember { mutableStateOf<String?>(null) }
    var fieldErrorPhaseIndex by remember { mutableStateOf<Int?>(null) }
    var clientPrimaryNumberError by remember { mutableStateOf<String?>(null) }
    var clientSecondaryNumberError by remember { mutableStateOf<String?>(null) }

    // LazyListState for auto-scrolling to error fields
    val listState = rememberLazyListState()

    // Track scroll position to show/hide title in header
    val showTitleInHeader by remember {
        derivedStateOf {
            // Show title in header when scrolled past the title item or when title starts scrolling out
            listState.firstVisibleItemIndex > 0 || (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset > 50)
        }
    }

    // Track positions of form fields for auto-scroll
    val fieldPositions = remember { mutableStateMapOf<String, Float>() }

    // Track which reviewData we've loaded to prevent re-loading the same data
    var loadedReviewDataKey by remember { mutableStateOf<String?>(null) }
    val mintColor = MaterialTheme.colorScheme.tertiary

    // Load draft by ID from Firestore if selected from DraftProjectsScreen
    LaunchedEffect(selectedDraftId) {
        android.util.Log.d(
            "NewProjectScreen",
            "🔄 LaunchedEffect(selectedDraftId) triggered - selectedDraftId: $selectedDraftId, lastLoadedDraftId: $lastLoadedDraftId"
        )
        if (selectedDraftId != null) {
            isDraftLoaded = false
            val draftId = selectedDraftId
            if (draftId != lastLoadedDraftId) {
                android.util.Log.d("NewProjectScreen", "🔄 Starting to load Firestore draft: $draftId")
                kotlinx.coroutines.delay(50)

                // Clear form first
                projectName = ""
                description = ""
                client = ""
                location = ""
                currency = "INR"
                plannedStartDate = null
                startDate = null
                endDate = null
                handoverDate = null
                maintenanceDate = null
                selectedApprover = null
                selectedTeamMembers = emptyList()
                selectedManager = null
                if (!isTemplateLoaded) phases.clear()
                projectCategories = emptyList()
                initialUserAssignments = emptyMap()
                initialApproverAssignments = emptyMap()
                finalUserAssignments = emptyMap()
                finalApproverAssignments = emptyMap()
                showApproverSearch = false
                showTeamMemberSearch = false
                approverSearchQuery = ""
                teamMemberSearchQuery = ""
                showPlannedStartDatePicker = false
                showStartDatePicker = false
                showEndDatePicker = false
                showHandoverDatePicker = false
                showMaintenanceDatePicker = false
                showMaintenanceDateDropdown = false
                selectedMaintenanceOption = null
                plannedStartDatePickerState.selectedDateMillis = null
                startDatePickerState.selectedDateMillis = null
                endDatePickerState.selectedDateMillis = null
                handoverDatePickerState.selectedDateMillis = null
                maintenanceDatePickerState.selectedDateMillis = null

                val draft = viewModel.loadFirestoreDraftById(draftId)
                if (draft != null) {
                    android.util.Log.d("NewProjectScreen", "📥 Populating form from Firestore draft: $draftId")
                    projectName = draft.projectName
                    description = draft.description
                    client = draft.client
                    clientPrimaryNumber = draft.clientPrimaryNumber
                    clientSecondaryNumber = draft.clientSecondaryNumber
                    location = draft.location
                    currency = draft.currency.ifEmpty { "INR" }
                    plannedStartDate = draft.plannedStartDate?.let { Date(it) }
                    startDate = draft.startDate?.let { Date(it) }
                    endDate = draft.endDate?.let { Date(it) }
                    handoverDate = draft.handoverDate?.let { Date(it) }
                    maintenanceDate = draft.maintenanceDate?.let { Date(it) }

                    plannedStartDate?.let { plannedStartDatePickerState.selectedDateMillis = it.time }
                    startDate?.let { startDatePickerState.selectedDateMillis = it.time }
                    endDate?.let { endDatePickerState.selectedDateMillis = it.time }
                    handoverDate?.let { handoverDatePickerState.selectedDateMillis = it.time }
                    maintenanceDate?.let { maintenanceDatePickerState.selectedDateMillis = it.time }

                    if (!isTemplateLoaded) phases.clear()
                    if (draft.phases.isNotEmpty()) {
                        draft.phases.forEach { serializablePhase ->
                            phases.add(
                                PhaseDraft(
                                    phaseName = serializablePhase.phaseName,
                                    start = serializablePhase.startDate?.let { Date(it) },
                                    end = serializablePhase.endDate?.let { Date(it) },
                                    departments = serializablePhase.departments.map { deptBudget ->
                                        com.cubiquitous.tracura.model.DepartmentDraft(
                                            departmentName = deptBudget.departmentName,
                                            contractorMode = com.cubiquitous.tracura.model.ContractorMode.LABOUR_ONLY,
                                            lineItems = if (deptBudget.allocatedBudget > 0) {
                                                mutableListOf(com.cubiquitous.tracura.model.LineItem(
                                                    itemType = "Budget",
                                                    item = "Total Budget",
                                                    spec = "",
                                                    quantity = 1.0,
                                                    unitPrice = deptBudget.allocatedBudget
                                                ))
                                            } else mutableListOf()
                                        )
                                    }.toMutableList(),
                                    categories = serializablePhase.categories.toMutableList()
                                )
                            )
                        }
                    } else {
                        phases.add(PhaseDraft())
                    }

                    projectCategories = draft.projectCategories.toMutableList()

                    // Restore selected manager, approver, and team members via pending UIDs
                    // (resolved to User objects once available lists are populated)
                    pendingLocalManagerUid = draft.managerUid
                    pendingLocalApproverUid = draft.approverUid
                    pendingLocalTeamMemberUids = draft.teamMemberUids

                    isDraftLoaded = true
                    lastLoadedDraftId = draftId
                    android.util.Log.d("NewProjectScreen", "✅ Firestore draft fields populated")
                    Toast.makeText(context, "Draft loaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    android.util.Log.e("NewProjectScreen", "❌ Firestore draft not found: $draftId")
                    lastLoadedDraftId = draftId
                    isDraftLoaded = true
                    Toast.makeText(context, "Draft not found", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            lastLoadedDraftId = null
        }
    }

    // Reconstruct approver and team members when they become available after draft load
    LaunchedEffect(selectedDraftId, availableApprovers, availableUsers) {
        if (justSubmittedProject) return@LaunchedEffect
        selectedDraftId?.let { draftId ->
            val draft = viewModel.firestoreDrafts.value.find { it.id == draftId }
                ?: viewModel.loadFirestoreDraftById(draftId)
            if (draft != null) {
                if (draft.approverUid != null && draft.approverUid.isNotEmpty() && availableApprovers.isNotEmpty() && selectedApprover == null) {
                    val approver = availableApprovers.find { it.phone == draft.approverUid }
                        ?: availableApprovers.find { it.uid.isNotEmpty() && it.uid == draft.approverUid }
                    if (approver != null) {
                        selectedApprover = approver
                        android.util.Log.d("NewProjectScreen", "✅ Approver loaded: ${approver.name}")
                    }
                }
                if (draft.teamMemberUids.isNotEmpty() && availableUsers.isNotEmpty() && selectedTeamMembers.isEmpty()) {
                    val teamMembers = draft.teamMemberUids.mapNotNull { phone ->
                        availableUsers.find { it.phone == phone }
                            ?: availableUsers.find { it.uid.isNotEmpty() && it.uid == phone }
                    }
                    if (teamMembers.isNotEmpty()) {
                        selectedTeamMembers = teamMembers
                        android.util.Log.d("NewProjectScreen", "✅ Team members loaded: ${teamMembers.size}")
                    }
                }
            }
        }
    }

    // Mark isDraftLoaded=true on a fresh form open (no selectedDraftId, no template)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        if (!isDraftLoaded && selectedDraftId == null) {
            isDraftLoaded = true
        }
    }

    // Load data from reviewData when available (e.g., when returning from Review screen via Edit button)
    // This happens when user clicks "Edit" on Review screen and navigates back
    LaunchedEffect(reviewData.projectName) {
        // Don't load reviewData if we just submitted a project (to prevent reloading after successful submission)
        if (justSubmittedProject) {
            android.util.Log.d(
                "NewProjectScreen",
                "⏸️ [REVIEW DATA] Skipping load - just submitted project (flag: true)"
            )
            return@LaunchedEffect
        }

        // Additional safeguard: Don't load if form is empty (indicating we just cleared it after submission)
        // This prevents reloading even if justSubmittedProject flag timing is off
        if (projectName.isEmpty() && reviewData.projectName.isNotEmpty()) {
            android.util.Log.d(
                "NewProjectScreen",
                "⏸️ [REVIEW DATA] Skipping load - form is empty but reviewData exists (likely just cleared after submission)"
            )
            android.util.Log.d(
                "NewProjectScreen",
                "⏸️ [REVIEW DATA] projectName: '$projectName', reviewData.projectName: '${reviewData.projectName}'"
            )
            return@LaunchedEffect
        }

        // Create a unique key from reviewData to detect changes
        val reviewDataKey = if (reviewData.projectName.isNotEmpty()) {
            "${reviewData.projectName}_${reviewData.phases.size}_${reviewData.teamMembers.size}"
        } else {
            null
        }

        // Load if we have reviewData and haven't loaded this specific data yet
        // Don't load if template is already loaded
        if (reviewData.projectName.isNotEmpty() && loadedReviewDataKey != reviewDataKey && activeTemplate == null) {
            android.util.Log.d(
                "NewProjectScreen",
                "📥 [REVIEW DATA] Loading data from reviewData: ${reviewData.projectName}"
            )
            android.util.Log.d(
                "NewProjectScreen",
                "📥 [REVIEW DATA] justSubmittedProject: $justSubmittedProject, loadedReviewDataKey: $loadedReviewDataKey, reviewDataKey: $reviewDataKey"
            )

            // Populate all form fields from reviewData
            projectName = reviewData.projectName
            description = reviewData.description
            client = reviewData.client
            location = reviewData.location
            currency = reviewData.currency.ifEmpty { "INR" }
            selectedApprover = reviewData.approver
            selectedTeamMembers = reviewData.teamMembers.toList()

            // Clear existing phases and populate from reviewData
            // Don't clear if template is loaded
            if (!isTemplateLoaded) {
                phases.clear()
            }
            reviewData.phases.forEach { phase ->
                phases.add(
                    PhaseDraft(
                        phaseName = phase.phaseName,
                        start = phase.start,
                        end = phase.end,
                        departments = phase.departments.toMutableList(),
                        categories = phase.categories.toMutableList()
                    )
                )
            }

            // If no phases in reviewData, ensure at least one empty phase exists
            if (phases.isEmpty()) {
                phases.add(PhaseDraft())
            }

            loadedReviewDataKey = reviewDataKey
            android.util.Log.d(
                "NewProjectScreen",
                "Successfully loaded reviewData. Phases: ${phases.size}, Approver: ${reviewData.approver?.name}, Team Members: ${reviewData.teamMembers.size}"
            )
        } else if (reviewData.projectName.isEmpty()) {
            // Reset key when reviewData is cleared
            loadedReviewDataKey = null
        }
    }

    // Handle success - Clear form fields but keep reviewData intact
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            android.util.Log.d(
                "NewProjectScreen",
                "🔄 [SUCCESS HANDLER] Processing success message - clearing form fields only"
            )
            android.util.Log.d(
                "NewProjectScreen",
                "🔄 [SUCCESS HANDLER] Current projectName: '$projectName', reviewData.projectName: '${reviewData.projectName}'"
            )

            // Set flag IMMEDIATELY to prevent any LaunchedEffects from reloading data
            justSubmittedProject = true
            viewModel.clearReviewData()
            android.util.Log.d(
                "NewProjectScreen",
                "✅ [SUCCESS HANDLER] justSubmittedProject flag set to true (preventing reload)"
            )

            // Clear draft data from Firestore if we just submitted from a draft
            selectedDraftId?.let { draftId ->
                viewModel.deleteFirestoreDraft(draftId)
            }
            android.util.Log.d("NewProjectScreen", "✅ [SUCCESS HANDLER] Draft cleared")

            // Clear all form fields to reset form to initial state (FAST - no delays)
            projectName = ""
            description = ""
            client = ""
            location = ""
            currency = "INR"
            plannedStartDate = null
            handoverDate = null
            maintenanceDate = null
            startDate = null
            endDate = null
            selectedApprover = null
            selectedTeamMembers = emptyList()
            selectedManager = null

            // Clear phases and add one empty phase
            phases.clear()
            phases.add(PhaseDraft())

            // Clear categories
            projectCategories = emptyList()
            initialUserAssignments = emptyMap()
            initialApproverAssignments = emptyMap()
            finalUserAssignments = emptyMap()
            finalApproverAssignments = emptyMap()

            // Clear template selection
            selectedTemplateInternal = null
            isTemplateLoaded = false
            templateDropdownExpanded = false

            // Reset expanded phases to first phase
            expandedPhases = setOf(0)

            // Clear field errors
            fieldError = null
            fieldErrorPhaseIndex = null

            // Clear UI states
            showApproverSearch = false
            showTeamMemberSearch = false
            approverSearchQuery = ""
            teamMemberSearchQuery = ""
            showPlannedStartDatePicker = false
            showHandoverDatePicker = false
            showMaintenanceDatePicker = false
            showStartDatePicker = false
            showEndDatePicker = false
            showMaintenanceDateDropdown = false
            selectedMaintenanceOption = null
            handoverDateError = null
            maintenanceDateError = null

            // Reset date picker states
            plannedStartDatePickerState.selectedDateMillis = null
            handoverDatePickerState.selectedDateMillis = null
            maintenanceDatePickerState.selectedDateMillis = null
            startDatePickerState.selectedDateMillis = null
            endDatePickerState.selectedDateMillis = null

            // Reset draft loaded state - set to true to prevent autosave from reloading
            isDraftLoaded = true
            lastLoadedDraftId = null

            // Reset loadedReviewDataKey to prevent reviewData from reloading
            loadedReviewDataKey = null

            // Clear local autosave after successful submission so new form starts clean.
            clearLocalAutosaveDraft()

            android.util.Log.d(
                "NewProjectScreen",
                "✅ [SUCCESS HANDLER] Form fields cleared - projectName: '$projectName', phases: ${phases.size}"
            )
            android.util.Log.d(
                "NewProjectScreen",
                "✅ [SUCCESS HANDLER] reviewData.projectName (kept intact): '${reviewData.projectName}'"
            )

            // Clear success message and navigate immediately (FAST)
            viewModel.clearSuccessMessage()
            android.util.Log.d(
                "NewProjectScreen", "✅ [SUCCESS HANDLER] Success message cleared, navigating back"
            )

            // Navigate back immediately (no delay for speed)
            onProjectCreated()

            // Reset the flag after navigation completes (longer delay to ensure recomposition doesn't reload)
            kotlinx.coroutines.delay(3000) // Increased to 3 seconds to ensure all recompositions complete
            justSubmittedProject = false
            android.util.Log.d(
                "NewProjectScreen",
                "✅ [SUCCESS HANDLER] justSubmittedProject flag reset - autosave can resume"
            )
        }
    }

    // Reset justSubmittedProject flag when user starts entering data
    LaunchedEffect(projectName, description, client, location) {
        // If user starts typing in any field, reset the flag to allow autosave
        if (justSubmittedProject && (projectName.isNotEmpty() || description.isNotEmpty() || client.isNotEmpty() || location.isNotEmpty())) {
            justSubmittedProject = false
            android.util.Log.d(
                "NewProjectScreen", "✅ User started entering data - justSubmittedProject flag reset"
            )
        }
    }

    // Autosave: Persist complete form locally whenever fields change (debounced).
    LaunchedEffect(
        hasAttemptedLocalRestore,
        selectedDraftId,
        projectIdForResubmission,
        justSubmittedProject,
        projectName,
        description,
        client,
        clientPrimaryNumber,
        clientSecondaryNumber,
        location,
        currency,
        plannedStartDate?.time,
        startDate?.time,
        endDate?.time,
        handoverDate?.time,
        maintenanceDate?.time,
        selectedMaintenanceOption,
        selectedManager?.uid,
        selectedApprover?.uid,
        selectedTeamMembers.mapNotNull { it.uid },
        phases.toList(),
        projectCategories,
        categoryName,
        finalUserAssignments,
        finalApproverAssignments,
        selectedTemplateInternal?.id
    ) {
        if (!hasAttemptedLocalRestore) return@LaunchedEffect
        if (selectedDraftId != null || projectIdForResubmission != null || justSubmittedProject) return@LaunchedEffect

        delay(800)

        persistLocalAutosaveSnapshot()
    }

    DisposableEffect(
        lifecycleOwner,
        hasAttemptedLocalRestore,
        selectedDraftId,
        projectIdForResubmission,
        justSubmittedProject,
        projectName,
        description,
        client,
        clientPrimaryNumber,
        clientSecondaryNumber,
        location,
        currency,
        plannedStartDate?.time,
        startDate?.time,
        endDate?.time,
        handoverDate?.time,
        maintenanceDate?.time,
        selectedMaintenanceOption,
        selectedManager?.uid,
        selectedApprover?.uid,
        selectedTeamMembers.mapNotNull { it.uid },
        phases.toList(),
        projectCategories,
        categoryName,
        finalUserAssignments,
        finalApproverAssignments,
        selectedTemplateInternal?.id
    ) {
        if (justSubmittedProject) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                    if (
                        hasAttemptedLocalRestore &&
                        selectedDraftId == null &&
                        projectIdForResubmission == null &&
                        !justSubmittedProject
                    ) {
                        persistLocalAutosaveSnapshot()
                    }
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                if (
                    hasAttemptedLocalRestore &&
                    selectedDraftId == null &&
                    projectIdForResubmission == null &&
                    !justSubmittedProject
                ) {
                    persistLocalAutosaveSnapshot()
                }
            }
        }
    }


    // Date formatters
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())


    // Auto-scroll to error field when field error occurs
    LaunchedEffect(fieldError, fieldErrorPhaseIndex) {
        fieldError?.let { fieldName ->
            // Small delay to ensure the error message and LazyColumn have rendered
            kotlinx.coroutines.delay(300)

            try {
                // Calculate the item index based on field type
                // LazyColumn structure: 0=TopBar, 1=PROJECT DETAILS, then phases (2, 3, ...), then TEAM ASSIGNMENT
                val itemIndex = when {
                    fieldName == "projectName" || fieldName == "description" || fieldName == "client" || fieldName == "location" || fieldName == "currency" -> 1 // PROJECT DETAILS section
                    fieldName == "approver" || fieldName == "teamMembers" -> {
                        // TEAM ASSIGNMENT section comes after all phases
                        // Index = 1 (PROJECT DETAILS) + phases.size + 1 (TEAM ASSIGNMENT)
                        1 + phases.size + 1
                    }

                    fieldName.startsWith("phaseName_") || fieldName.startsWith("phaseNameDuplicate_") || fieldName.startsWith(
                        "startDate_"
                    ) || fieldName.startsWith("endDate_") || fieldName.startsWith("departments_") || fieldName.startsWith(
                        "departmentsDuplicate_"
                    ) -> {
                        // Phase field - find the phase index
                        val phaseIndex = fieldErrorPhaseIndex ?: 0
                        // Index = 1 (PROJECT DETAILS) + phaseIndex + 1 (the phase itself)
                        1 + phaseIndex + 1
                    }

                    else -> {
                        // Default to first phase or PROJECT DETAILS
                        val phaseIndex = fieldErrorPhaseIndex ?: 0
                        1 + phaseIndex + 1
                    }
                }

                // Scroll to the section containing the error field
                // Calculate scroll offset based on field position if available
                val fieldPosition = fieldPositions[fieldName]
                val scrollOffset = if (fieldPosition != null && fieldPosition > 0) {
                    // Convert pixel position to dp offset (approximately 3.5 pixels per dp)
                    // Add some padding (28dp ≈ 100 pixels) to show the field label clearly
                    ((fieldPosition - 100f) / 3.5f).coerceAtLeast(0f).toInt()
                } else {
                    0
                }

                listState.animateScrollToItem(
                    index = itemIndex.coerceAtMost(listState.layoutInfo.totalItemsCount - 1),
                    scrollOffset = scrollOffset
                )
            } catch (e: Exception) {
                // If scrolling fails, try to scroll to the first item
                try {
                    listState.animateScrollToItem(0)
                } catch (ex: Exception) {
                    // Ignore scroll errors
                }
            }
        }
    }

    val isResubmissionLoading = projectIdForResubmission != null && !isProjectDataLoaded

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
                // Fixed Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow, shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onNavigateBack) {
                            Text(
                                "Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        // Show title in header when scrolled
                        AnimatedVisibility(
                            visible = showTitleInHeader,
                            enter = fadeIn() + slideInHorizontally(),
                            exit = fadeOut() + slideOutHorizontally()
                        ) {

                            Text(
                                text = if (projectIdForResubmission != null) "Edit Project" else "New Project",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                        }


                        Spacer(modifier = Modifier.weight(1f))

                        // Refresh/Clear icon
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f), CircleShape
                                )
                                .clickable {
                                    // Clear all fields
                                    projectName = ""
                                    description = ""
                                    client = ""
                                    location = ""
                                    currency = "INR"
                                    plannedStartDate = null
                                    startDate = null
                                    endDate = null
                                    selectedApprover = null
                                    selectedManager = null
                                    managerSearchQuery = ""
                                    showManagerDropdown = false
                                    selectedTeamMembers = emptyList()
                                    phases.clear()
                                    phases.add(PhaseDraft())
                                    projectCategories = emptyList()
                                    initialUserAssignments = emptyMap()
                                    initialApproverAssignments = emptyMap()
                                    finalUserAssignments = emptyMap()
                                    finalApproverAssignments = emptyMap()

                                    // Clear UI states
                                    showApproverSearch = false
                                    showTeamMemberSearch = false
                                    approverSearchQuery = ""
                                    teamMemberSearchQuery = ""
                                    showPlannedStartDatePicker = false
                                    showStartDatePicker = false
                                    showEndDatePicker = false

                                    // Clear draft from Firestore
                                    selectedDraftId?.let { draftId ->
                                        viewModel.deleteFirestoreDraft(draftId)
                                    }

                                    // Reset date picker states
                                    plannedStartDatePickerState.selectedDateMillis = null
                                    startDatePickerState.selectedDateMillis = null
                                    endDatePickerState.selectedDateMillis = null

                                    clearLocalAutosaveDraft()

                                    Toast.makeText(
                                        context, "All fields cleared", Toast.LENGTH_SHORT
                                    ).show()
                                }, contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Clear All Fields",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Download/Save icon - Save current form as draft
                        IconButton(
                            onClick = {
                                // Save current form state as draft to Firestore
                                viewModel.saveDraftToFirestore(
                                    projectName = projectName,
                                    description = description,
                                    client = client,
                                    clientPrimaryNumber = clientPrimaryNumber.takeIf { it.isNotBlank() },
                                    clientSecondaryNumber = clientSecondaryNumber.takeIf { it.isNotBlank() },
                                    location = location,
                                    currency = currency,
                                    plannedStartDate = plannedStartDate?.time,
                                    startDate = startDate?.time,
                                    endDate = endDate?.time,
                                    handoverDate = handoverDate?.time,
                                    maintenanceDate = maintenanceDate?.time,
                                    managerUid = selectedManager?.phone,
                                    approverUid = selectedApprover?.phone,
                                    teamMemberUids = selectedTeamMembers.map { it.phone }.filter { it.isNotEmpty() },
                                    phases = phases.toList(),
                                    projectCategories = projectCategories,
                                    onSuccess = {
                                        // Clear all fields after saving draft
                                        projectName = ""
                                        description = ""
                                        client = ""
                                        location = ""
                                        currency = "INR"
                                        plannedStartDate = null
                                        startDate = null
                                        endDate = null
                                        handoverDate = null
                                        maintenanceDate = null
                                        selectedApprover = null
                                        selectedManager = null
                                        selectedTeamMembers = emptyList()
                                        phases.clear()
                                        phases.add(PhaseDraft())
                                        projectCategories = emptyList()
                                        initialUserAssignments = emptyMap()
                                        initialApproverAssignments = emptyMap()
                                        finalUserAssignments = emptyMap()
                                        finalApproverAssignments = emptyMap()

                                        // Clear UI states
                                        showApproverSearch = false
                                        showTeamMemberSearch = false
                                        approverSearchQuery = ""
                                        teamMemberSearchQuery = ""
                                        showPlannedStartDatePicker = false
                                        showStartDatePicker = false
                                        showEndDatePicker = false
                                        showHandoverDatePicker = false
                                        showMaintenanceDatePicker = false
                                        showMaintenanceDateDropdown = false
                                        selectedMaintenanceOption = null

                                        // Reset date picker states
                                        plannedStartDatePickerState.selectedDateMillis = null
                                        startDatePickerState.selectedDateMillis = null
                                        endDatePickerState.selectedDateMillis = null
                                        handoverDatePickerState.selectedDateMillis = null
                                        maintenanceDatePickerState.selectedDateMillis = null

                                        // Reset draft loaded state
                                        isDraftLoaded = false

                                        clearLocalAutosaveDraft()

                                        Toast.makeText(
                                            context,
                                            "Draft saved successfully. All fields cleared.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onError = { error ->
                                        Toast.makeText(
                                            context,
                                            "Failed to save draft: $error",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }) {
                            if (isSavingDraft) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = "Save Draft",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = listState
                ) {
                    // Rejection Reason Display (if project is REVIEW REJECTED)
                    editingProject?.let { project ->
                        if (project.status == "REVIEW REJECTED" && !project.rejectionReason.isNullOrBlank() && projectIdForResubmission != null) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(
                                            0xFFFFEBEE
                                        )
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "Rejection Reason",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Text(
                                            text = project.rejectionReason ?: "",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 20.sp
                                        )
                                        Text(
                                            text = "Please review and update the project details, then resubmit for approval.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            // New Project Header - below top bar (hide when scrolled)
                            AnimatedVisibility(
                                visible = !showTitleInHeader,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, bottom = 24.dp)
                                ) {
                                    Text(
                                        text = if (projectIdForResubmission != null) "Edit Project" else "New Project",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Template Selection Dropdown
                                    Spacer(modifier = Modifier.height(16.dp))
                                    ExposedDropdownMenuBox(
                                        expanded = templateDropdownExpanded,
                                        onExpandedChange = { templateDropdownExpanded = it }) {
                                        // Gradient background brush (blue to purple with 0.15 opacity)
                                        val gradientBrush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) // Purple
                                            ), start = Offset(0f, 0f), // topLeading
                                            end = Offset(
                                                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY
                                            ) // bottomTrailing
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 100.dp)
                                                .background(
                                                    brush = gradientBrush,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                        ) {
                                            OutlinedTextField(
                                                value = activeTemplate?.name ?: "",
                                                onValueChange = { },
                                                readOnly = true,
                                                placeholder = {
                                                    Text(
                                                        "Select a template",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                leadingIcon = {
                                                    if (activeTemplate != null) {
                                                        Icon(
                                                            activeTemplate.icon,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                    } else {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.template),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                    }
                                                },
                                                trailingIcon = {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            1.dp
                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    ) {
                                                        if (activeTemplate != null) {
                                                            IconButton(
                                                                onClick = {
                                                                    selectedTemplateInternal = null
                                                                    templateDropdownExpanded = false
                                                                    // Clear template loaded flag to allow re-selection
                                                                    isTemplateLoaded = false
                                                                    // Clear phases
                                                                    phases.clear()
                                                                    phases.add(PhaseDraft())
                                                                }, modifier = Modifier.size(24.dp)
                                                            ) {
                                                                // Intentionally left blank – icon removed for cleaner UI
                                                            }
                                                        }
                                                        Icon(
                                                            Icons.Default.KeyboardArrowDown,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier
                                                                .size(20.dp)
                                                                .rotate(if (templateDropdownExpanded) 180f else 0f)
                                                        )
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .menuAnchor()/*.padding(start = 70.dp)*/
                                                    .clickable { templateDropdownExpanded = true },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedContainerColor = Color.Transparent,
                                                    focusedBorderColor = Color.Transparent,
                                                    unfocusedBorderColor = Color.Transparent,
                                                    focusedTextColor = if (activeTemplate != null) MaterialTheme.colorScheme.onSurface else Color(
                                                        0xFF8E8E93
                                                    ),
                                                    unfocusedTextColor = if (activeTemplate != null) MaterialTheme.colorScheme.onSurface else Color(
                                                        0xFF8E8E93
                                                    )
                                                )
                                            )
                                        }
                                        ExposedDropdownMenu(
                                            expanded = templateDropdownExpanded,
                                            onDismissRequest = { templateDropdownExpanded = false },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            if (availableTemplates.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("No templates available") },
                                                    onClick = { })
                                            } else {
                                                availableTemplates.forEachIndexed { index, template ->
                                                    val isSelected =
                                                        activeTemplate?.id == template.id
                                                    DropdownMenuItem(
                                                        text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(
                                                                    12.dp
                                                                ),
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Icon(
                                                                    template.icon,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                                Text(
                                                                    text = template.name,
                                                                    color = if (isSelected) Color(
                                                                        0xFF007AFF
                                                                    ) else MaterialTheme.colorScheme.onSurface,
                                                                    fontSize = 17.sp,
                                                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                                                )
                                                            }
                                                            if (isSelected) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = "Selected",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    }, onClick = {
                                                        // Check if form has user-filled data before allowing template change
                                                        val hasUserData = projectName.isNotBlank() || 
                                                                         description.isNotBlank() || 
                                                                         client.isNotBlank() || 
                                                                         location.isNotBlank() ||
                                                                         phases.any { phase -> 
                                                                             phase.phaseName.isNotBlank() || 
                                                                             phase.departments.isNotEmpty() ||
                                                                             phase.start != null ||
                                                                             phase.end != null
                                                                         }
                                                        
                                                        if (hasUserData) {
                                                            // Don't change template if user has filled data
                                                            android.util.Log.d(
                                                                "NewProjectScreen",
                                                                "⏸️ Template change blocked - form has user-filled data"
                                                            )
                                                            templateDropdownExpanded = false
                                                        } else {
                                                            // Only allow template change if form is empty
                                                            selectedTemplateInternal = template
                                                            templateDropdownExpanded = false
                                                            // Reset template loaded flag to allow loading
                                                            isTemplateLoaded = false
                                                            android.util.Log.d(
                                                                "NewProjectScreen",
                                                                "✅ Template selected: ${template.name}"
                                                            )
                                                        }
                                                    }, contentPadding = PaddingValues(
                                                        horizontal = 16.dp, vertical = 12.dp
                                                    ), colors = MenuDefaults.itemColors(
                                                        textColor = if (isSelected) Color(
                                                            0xFF007AFF
                                                        ) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    )
                                                    if (index < availableTemplates.size - 1) {
                                                        HorizontalDivider(
                                                            modifier = Modifier.padding(horizontal = 0.dp),
                                                            color = MaterialTheme.colorScheme.outlineVariant,
                                                            thickness = 1.dp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // View Draft Project Creations Banner
                            if (firestoreDrafts.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToDraftProjects() }
                                        .padding(bottom = 16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(
                                            0xFFE3F2FD
                                        )
                                    ), // Light blue background
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 16.dp, vertical = 12.dp
                                            ), // Reduced vertical padding
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary, // Blue icon to match text
                                                modifier = Modifier.size(20.dp) // Slightly smaller icon
                                            )
                                            Text(
                                                text = "View Draft Project Creations",
                                                fontSize = 15.sp, // Slightly smaller font
                                                fontWeight = FontWeight.Normal, // Regular weight instead of Medium
                                                color = MaterialTheme.colorScheme.primary // Blue text color
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary, // Blue arrow to match
                                            modifier = Modifier.size(18.dp) // Slightly smaller arrow
                                        )
                                    }
                                }
                            }

                            // PROJECT DETAILS Section - iOS White Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Section Header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Project icon with blue circular background
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary, CircleShape
                                                ), contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Business,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "PROJECT DETAILS",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Template description (if template is selected)
                                    activeTemplate?.let { template ->
                                        val templateDescription =
                                            getTemplateDescription(template.name)
                                        Text(
                                            text = templateDescription,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coordinates ->
                                                fieldPositions["projectName"] =
                                                    coordinates.positionInParent().y
                                            }) {
                                        Text(
                                            text = "Project Name",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        OutlinedTextField(
                                            value = projectName,
                                            onValueChange = {
                                                projectName = it
                                                // Clear error when user starts typing
                                                if (fieldError == "projectName") {
                                                    fieldError = null
                                                }
                                            },
                                            placeholder = {
                                                Text(
                                                    "Enter project name", color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            isError = fieldError == "projectName",
                                            supportingText = if (fieldError == "projectName") {
                                                {
                                                    Text(
                                                        "Project name is required",
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            } else null,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedBorderColor = if (fieldError == "projectName") Color(
                                                    0xFFD32F2F
                                                ) else MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = if (fieldError == "projectName") Color(
                                                    0xFFD32F2F
                                                ) else MaterialTheme.colorScheme.outline,
                                                errorBorderColor = MaterialTheme.colorScheme.error
                                            ))
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 1.dp
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coordinates ->
                                                fieldPositions["description"] =
                                                    coordinates.positionInParent().y
                                            }) {
                                        Text(
                                            text = "Description",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        OutlinedTextField(
                                            value = description,
                                            onValueChange = { newText ->
                                                // Count words
                                                val words = newText.trim().split("\\s+".toRegex())
                                                    .filter { it.isNotEmpty() }
                                                val wordCount = words.size

                                                // Only update if within word limit
                                                if (wordCount <= maxDescriptionWords) {
                                                    description = newText
                                                    // Clear error when user starts typing
                                                    if (fieldError == "description" || fieldError == "descriptionWordLimit") {
                                                        fieldError = null
                                                    }
                                                } else {
                                                    // Show error but don't update text
                                                    fieldError = "descriptionWordLimit"
                                                }
                                            },
                                            placeholder = {
                                                Text(
                                                    "Enter description (max 150 words)",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            minLines = 3,
                                            isError = fieldError == "description" || fieldError == "descriptionWordLimit",
                                            supportingText = {
                                                when {
                                                    fieldError == "description" -> {
                                                        Text(
                                                            "Description is required",
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }

                                                    fieldError == "descriptionWordLimit" -> {
                                                        Text(
                                                            "Description must not exceed 150 words. Current: $descriptionWordCount/$maxDescriptionWords",
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }

                                                    else -> {
                                                        Text(
                                                            "$descriptionWordCount/$maxDescriptionWords words",
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedBorderColor = when {
                                                    fieldError == "description" || fieldError == "descriptionWordLimit" -> Color(
                                                        0xFFD32F2F
                                                    )

                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                                unfocusedBorderColor = when {
                                                    fieldError == "description" || fieldError == "descriptionWordLimit" -> Color(
                                                        0xFFD32F2F
                                                    )

                                                    else -> MaterialTheme.colorScheme.outline
                                                },
                                                errorBorderColor = MaterialTheme.colorScheme.error
                                            )
                                        )
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 1.dp
                                    )

                                    // Client
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coordinates ->
                                                fieldPositions["client"] =
                                                    coordinates.positionInParent().y
                                            }) {
                                        Text(
                                            text = "Client",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        OutlinedTextField(
                                            value = client,
                                            onValueChange = {
                                                client = it
                                                // Clear error when user starts typing
                                                if (fieldError == "client") {
                                                    fieldError = null
                                                }
                                            },
                                            placeholder = {
                                                Text(
                                                    "Enter client name", color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            isError = fieldError == "client",
                                            supportingText = if (fieldError == "client") {
                                                {
                                                    Text(
                                                        "Client is required",
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            } else null,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedBorderColor = if (fieldError == "client") Color(
                                                    0xFFD32F2F
                                                ) else MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = if (fieldError == "client") Color(
                                                    0xFFD32F2F
                                                ) else MaterialTheme.colorScheme.outline,
                                                errorBorderColor = MaterialTheme.colorScheme.error
                                            ))
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 1.dp
                                    )

                                    // Client Primary Number
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                                            Text(
                                                text = "Client Primary Number",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = " *",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            // Country code selector
                                            ExposedDropdownMenuBox(
                                                expanded = primaryCountryCodeExpanded,
                                                onExpandedChange = { primaryCountryCodeExpanded = it },
                                                modifier = Modifier.width(130.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = "${primaryCountryCode.flag} ${primaryCountryCode.dialCode}",
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    modifier = Modifier.menuAnchor(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    trailingIcon = {
                                                        Icon(
                                                            imageVector = if (primaryCountryCodeExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.ArrowDropDown,
                                                            contentDescription = null,
                                                            modifier = Modifier.rotate(if (primaryCountryCodeExpanded) 180f else 0f)
                                                        )
                                                    },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                                    )
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = primaryCountryCodeExpanded,
                                                    onDismissRequest = { primaryCountryCodeExpanded = false }
                                                ) {
                                                    allCountryCodes.forEach { country ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text("${country.flag} ${country.name} (${country.dialCode})", fontSize = 14.sp)
                                                            },
                                                            onClick = {
                                                                primaryCountryCode = country
                                                                primaryCountryCodeExpanded = false
                                                                // Re-validate with new country rules
                                                                val digits = clientPrimaryNumber.filter { it.isDigit() }
                                                                clientPrimaryNumberError = when {
                                                                    clientPrimaryNumber.isEmpty() -> "Primary contact number is required"
                                                                    digits.length < country.minDigits -> "Must be ${country.minDigits} digits for ${country.name}"
                                                                    digits.length > country.maxDigits -> "Cannot exceed ${country.maxDigits} digits for ${country.name}"
                                                                    clientPrimaryNumber == clientSecondaryNumber && clientPrimaryNumber.isNotEmpty() -> "Cannot be same as secondary number"
                                                                    else -> null
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                            // Number field
                                            OutlinedTextField(
                                                value = clientPrimaryNumber,
                                                onValueChange = { newValue ->
                                                    val filtered = newValue.filter { it.isDigit() }
                                                    clientPrimaryNumber = filtered
                                                    val digits = filtered.filter { it.isDigit() }
                                                    clientPrimaryNumberError = when {
                                                        filtered.isEmpty() -> "Primary contact number is required"
                                                        digits.length < primaryCountryCode.minDigits -> "Must be ${primaryCountryCode.minDigits} digits for ${primaryCountryCode.name}"
                                                        digits.length > primaryCountryCode.maxDigits -> "Cannot exceed ${primaryCountryCode.maxDigits} digits for ${primaryCountryCode.name}"
                                                        filtered == clientSecondaryNumber && filtered.isNotEmpty() -> "Cannot be same as secondary number"
                                                        else -> null
                                                    }
                                                    if (clientSecondaryNumberError == "Cannot be same as primary number" && filtered != clientSecondaryNumber) {
                                                        clientSecondaryNumberError = null
                                                    }
                                                },
                                                placeholder = {
                                                    Text(
                                                        "Enter ${primaryCountryCode.minDigits}-digit number",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                isError = clientPrimaryNumberError != null,
                                                supportingText = if (clientPrimaryNumberError != null) {
                                                    {
                                                        Text(
                                                            clientPrimaryNumberError!!,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                } else null,
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    focusedBorderColor = if (clientPrimaryNumberError != null) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                                                    unfocusedBorderColor = if (clientPrimaryNumberError != null) Color(0xFFD32F2F) else MaterialTheme.colorScheme.outline,
                                                    errorBorderColor = MaterialTheme.colorScheme.error
                                                )
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 1.dp
                                    )

                                    // Client Secondary Number
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                                            Text(
                                                text = "Client Secondary Number",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = " (Optional)",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            // Country code selector
                                            ExposedDropdownMenuBox(
                                                expanded = secondaryCountryCodeExpanded,
                                                onExpandedChange = { secondaryCountryCodeExpanded = it },
                                                modifier = Modifier.width(130.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = "${secondaryCountryCode.flag} ${secondaryCountryCode.dialCode}",
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    modifier = Modifier.menuAnchor(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    trailingIcon = {
                                                        Icon(
                                                            imageVector = if (secondaryCountryCodeExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.ArrowDropDown,
                                                            contentDescription = null,
                                                            modifier = Modifier.rotate(if (secondaryCountryCodeExpanded) 180f else 0f)
                                                        )
                                                    },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                                    )
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = secondaryCountryCodeExpanded,
                                                    onDismissRequest = { secondaryCountryCodeExpanded = false }
                                                ) {
                                                    allCountryCodes.forEach { country ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text("${country.flag} ${country.name} (${country.dialCode})", fontSize = 14.sp)
                                                            },
                                                            onClick = {
                                                                secondaryCountryCode = country
                                                                secondaryCountryCodeExpanded = false
                                                                // Re-validate with new country rules
                                                                if (clientSecondaryNumber.isNotEmpty()) {
                                                                    val digits = clientSecondaryNumber.filter { it.isDigit() }
                                                                    clientSecondaryNumberError = when {
                                                                        digits.length < country.minDigits -> "Must be ${country.minDigits} digits for ${country.name}"
                                                                        digits.length > country.maxDigits -> "Cannot exceed ${country.maxDigits} digits for ${country.name}"
                                                                        clientSecondaryNumber == clientPrimaryNumber -> "Cannot be same as primary number"
                                                                        else -> null
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                            // Number field
                                            OutlinedTextField(
                                                value = clientSecondaryNumber,
                                                onValueChange = { newValue ->
                                                    val filtered = newValue.filter { it.isDigit() }
                                                    clientSecondaryNumber = filtered
                                                    clientSecondaryNumberError = when {
                                                        filtered.isEmpty() -> null
                                                        filtered.filter { it.isDigit() }.length < secondaryCountryCode.minDigits -> "Must be ${secondaryCountryCode.minDigits} digits for ${secondaryCountryCode.name}"
                                                        filtered.filter { it.isDigit() }.length > secondaryCountryCode.maxDigits -> "Cannot exceed ${secondaryCountryCode.maxDigits} digits for ${secondaryCountryCode.name}"
                                                        filtered == clientPrimaryNumber && filtered.isNotEmpty() -> "Cannot be same as primary number"
                                                        else -> null
                                                    }
                                                },
                                                placeholder = {
                                                    Text(
                                                        "Enter ${secondaryCountryCode.minDigits}-digit number",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                isError = clientSecondaryNumberError != null,
                                                supportingText = if (clientSecondaryNumberError != null) {
                                                    {
                                                        Text(
                                                            clientSecondaryNumberError!!,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                } else null,
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    focusedBorderColor = if (clientSecondaryNumberError != null) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                                                    unfocusedBorderColor = if (clientSecondaryNumberError != null) Color(0xFFD32F2F) else MaterialTheme.colorScheme.outline,
                                                    errorBorderColor = MaterialTheme.colorScheme.error
                                                )
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 1.dp
                                    )

                                    // Location
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coordinates ->
                                                fieldPositions["location"] =
                                                    coordinates.positionInParent().y
                                            }) {
                                        Text(
                                            text = "Location",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        OutlinedTextField(
                                            value = location,
                                            onValueChange = {
                                                location = it
                                                // Clear error when user starts typing
                                                if (fieldError == "location") {
                                                    fieldError = null
                                                }
                                            },
                                            placeholder = {
                                                Text(
                                                    "Enter location", color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            isError = fieldError == "location",
                                            supportingText = if (fieldError == "location") {
                                                {
                                                    Text(
                                                        "Location is required",
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            } else null,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedBorderColor = if (fieldError == "location") Color(
                                                    0xFFD32F2F
                                                ) else MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = if (fieldError == "location") Color(
                                                    0xFFD32F2F
                                                ) else MaterialTheme.colorScheme.outline,
                                                errorBorderColor = MaterialTheme.colorScheme.error
                                            ))
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 1.dp
                                    )

                                    // Planned Start Date
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coordinates ->
                                                fieldPositions["plannedStartDate"] =
                                                    coordinates.positionInParent().y
                                            }) {
                                        Text(
                                            text = "Planned Start Date",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        OutlinedTextField(
                                            value = plannedStartDate?.let {
                                            val displayFormatter = SimpleDateFormat(
                                                "MMM dd, yyyy", Locale.getDefault()
                                            )
                                            displayFormatter.format(it)
                                        } ?: "",
                                            onValueChange = { },
                                            readOnly = true,
                                            placeholder = {
                                                Text(
                                                    "Select start date", color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.DateRange,
                                                    contentDescription = "Select Date",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.clickable {
                                                        showPlannedStartDatePicker =
                                                            !showPlannedStartDatePicker
                                                    })
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showPlannedStartDatePicker =
                                                        !showPlannedStartDatePicker
                                                },
                                            isError = fieldError == "plannedStartDate",
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            )
                                        )

                                        // Planned date error message
                                        if (fieldError == "plannedStartDate") {
                                            Text(
                                                text = "Planned start date cannot be earlier than today",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                                            )
                                        }

                                        // Inline Date Picker
                                        if (showPlannedStartDatePicker) {
                                            // Capture the millis already set when the picker opens so we
                                            // only commit + close when the user taps a DIFFERENT date.
                                            val initialPickerMillis = remember { plannedStartDatePickerState.selectedDateMillis }
                                            LaunchedEffect(Unit) {
                                                snapshotFlow { plannedStartDatePickerState.selectedDateMillis }
                                                    .collect { millis ->
                                                        if (millis != null && millis != initialPickerMillis) {
                                                            // Ensure planned date is not before today
                                                            val cal = Calendar.getInstance()
                                                            cal.set(Calendar.HOUR_OF_DAY, 0)
                                                            cal.set(Calendar.MINUTE, 0)
                                                            cal.set(Calendar.SECOND, 0)
                                                            cal.set(Calendar.MILLISECOND, 0)
                                                            val todayStart = cal.timeInMillis

                                                            if (millis < todayStart) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Planned start date cannot be earlier than today",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                                // Revert picker selection
                                                                plannedStartDatePickerState.selectedDateMillis = initialPickerMillis
                                                            } else {
                                                                plannedStartDate = Date(millis)
                                                                showPlannedStartDatePicker = false
                                                            }
                                                        }
                                                    }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                shape = RoundedCornerShape(12.dp),
                                                elevation = CardDefaults.cardElevation(
                                                    defaultElevation = 4.dp
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp, vertical = 8.dp
                                                    )
                                                ) {
                                                    DatePicker(
                                                        state = plannedStartDatePickerState,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .wrapContentHeight(),
                                                        title = null,
                                                        headline = null,
                                                        showModeToggle = false,
                                                        colors = DatePickerDefaults.colors(
                                                            containerColor = MaterialTheme.colorScheme.surface,
                                                            selectedDayContainerColor = Color(
                                                                0xFF007AFF
                                                            ),
                                                            selectedDayContentColor = MaterialTheme.colorScheme.surface,
                                                            todayDateBorderColor = MaterialTheme.colorScheme.primary,
                                                            dayContentColor = MaterialTheme.colorScheme.onSurface,
                                                            weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            navigationContentColor = Color(
                                                                0xFF007AFF
                                                            ),
                                                            yearContentColor = MaterialTheme.colorScheme.primary,
                                                            currentYearContentColor = Color(
                                                                0xFF007AFF
                                                            )
                                                        )
                                                    )

                                                    // Show the currently-highlighted date label below calendar
                                                    plannedStartDatePickerState.selectedDateMillis?.let { millis ->
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        val displayFormatter = SimpleDateFormat(
                                                            "MMM dd, yyyy", Locale.getDefault()
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(
                                                                    MaterialTheme.colorScheme.surface,
                                                                    RoundedCornerShape(20.dp)
                                                                )
                                                                .padding(
                                                                    horizontal = 16.dp,
                                                                    vertical = 8.dp
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = displayFormatter.format(Date(millis)),
                                                                fontSize = 15.sp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Auto-select toggle for phases
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = "Auto-select phase dates",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Switch(
                                                checked = autoSelectPhaseDates,
                                                onCheckedChange = { checked -> autoSelectPhaseDates = checked },
                                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                                            )
                                        }

                                        Text(
                                            text = "Project will be in LOCKED status until this date, then automatically become ACTIVE.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 1.dp
                                    )

                                    // Currency Picker
                                    var currencyExpanded by remember { mutableStateOf(false) }
                                    val currencyOptions = listOf(
                                        "INR" to "₹ Indian Rupee",
                                        "USD" to "$ US Dollar",
                                        "EUR" to "€ Euro",
                                        "GBP" to "£ British Pound"
                                    )
                                    val currencySymbol = when (currency) {
                                        "INR" -> "₹"
                                        "USD" -> "$"
                                        "EUR" -> "€"
                                        "GBP" -> "£"
                                        else -> "₹"
                                    }
                                    val currencyDisplayName =
                                        currencyOptions.find { it.first == currency }?.second
                                            ?: "₹ Indian Rupee"

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coordinates ->
                                                fieldPositions["currency"] =
                                                    coordinates.positionInParent().y
                                            }) {
                                        Text(
                                            text = "Currency",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        ExposedDropdownMenuBox(
                                            expanded = currencyExpanded,
                                            onExpandedChange = { currencyExpanded = it }) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                OutlinedTextField(
                                                    value = currencyDisplayName,
                                                    onValueChange = { },
                                                    readOnly = true,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .menuAnchor()
                                                        .clickable {
                                                            currencyExpanded = true
                                                            // Clear error when user selects currency
                                                            if (fieldError == "currency") {
                                                                fieldError = null
                                                            }
                                                        },
                                                    shape = RoundedCornerShape(8.dp),
                                                    isError = fieldError == "currency",
                                                    supportingText = if (fieldError == "currency") {
                                                        {
                                                            Text(
                                                                "Currency is required",
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    } else null,
                                                    trailingIcon = {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                1.dp
                                                            ),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(end = 4.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.ChevronRight,
                                                                contentDescription = null,
                                                                tint = if (fieldError == "currency") Color(
                                                                    0xFFD32F2F
                                                                ) else MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier
                                                                    .size(12.dp)
                                                                    .rotate(-90f)
                                                            )
                                                            Icon(
                                                                Icons.Default.ChevronRight,
                                                                contentDescription = null,
                                                                tint = if (fieldError == "currency") Color(
                                                                    0xFFD32F2F
                                                                ) else MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier
                                                                    .size(12.dp)
                                                                    .rotate(90f)
                                                            )
                                                        }
                                                    },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                        focusedBorderColor = if (fieldError == "currency") Color(
                                                            0xFFD32F2F
                                                        ) else MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor = if (fieldError == "currency") Color(
                                                            0xFFD32F2F
                                                        ) else MaterialTheme.colorScheme.outline,
                                                        errorBorderColor = MaterialTheme.colorScheme.error,
                                                        focusedTextColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedTextColor = if (currency.isNotEmpty()) Color(
                                                            0xFF007AFF
                                                        ) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                            ExposedDropdownMenu(
                                                expanded = currencyExpanded,
                                                onDismissRequest = { currencyExpanded = false },
                                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                            ) {
                                                currencyOptions.forEachIndexed { index, (code, displayName) ->
                                                    DropdownMenuItem(
                                                        text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = displayName,
                                                                color = if (currency == code) Color(
                                                                    0xFF007AFF
                                                                ) else MaterialTheme.colorScheme.onSurface,
                                                                fontSize = 17.sp
                                                            )
                                                            if (currency == code) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    contentDescription = "Selected",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    }, onClick = {
                                                        currency = code
                                                        currencyExpanded = false
                                                        // Clear error when currency is selected
                                                        if (fieldError == "currency") {
                                                            fieldError = null
                                                        }
                                                    }, contentPadding = PaddingValues(
                                                        horizontal = 16.dp, vertical = 12.dp
                                                    ), colors = MenuDefaults.itemColors(
                                                        textColor = if (currency == code) Color(
                                                            0xFF007AFF
                                                        ) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    )
                                                    if (index < currencyOptions.size - 1) {
                                                        HorizontalDivider(
                                                            modifier = Modifier.padding(horizontal = 0.dp),
                                                            color = MaterialTheme.colorScheme.outlineVariant,
                                                            thickness = 1.dp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // PROJECT PHASES - List
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Phase icon with blue circular background
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary, CircleShape
                                        ), contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PROJECT PHASES",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            phases.forEachIndexed { index, phaseDraft ->
                                val isExpanded = expandedPhases.contains(index)
                                val isComplete =
                                    phaseDraft.phaseName.isNotEmpty() && phaseDraft.start != null && phaseDraft.end != null && phaseDraft.departments.isNotEmpty() && phaseDraft.departments.any { it.departmentName.isNotEmpty() }
                                val totalBudget = phaseDraft.departments.sumOf { it.totalBudget }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Section Header - Collapsible
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isExpanded) {
                                                        expandedPhases = expandedPhases - index
                                                    } else {
                                                        expandedPhases = expandedPhases + index
                                                    }
                                                }, verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "Phase ${index + 1}/${phases.size}",
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // Rupee icon with blue background
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.secondaryContainer, CircleShape
                                                            ), contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "₹",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Text(
                                                        text = "Budget:",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    // Gradient brush for green to mint
                                                    val budgetGradient = Brush.linearGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary, // Green
                                                            mintColor
                                                        ),
                                                        start = Offset(0f, 0f),
                                                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                                                    )

                                                    Text(
                                                        text = FormatUtils.formatCurrencyWithoutDecimals(
                                                            totalBudget
                                                        ), style = TextStyle(
                                                            fontSize = 20.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            brush = budgetGradient
                                                        )
                                                    )
                                                }
                                            }

                                            // Checkmark for complete phase
                                            if (isComplete) {
                                                // Gradient background (green to mint)
                                                val checkmarkGradient = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                        mintColor.copy(alpha = 0.1f)
                                                    )
                                                    //center = Offset(14f, 14f),
                                                    // radius = 14f
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .background(
                                                            brush = checkmarkGradient,
                                                            shape = CircleShape
                                                        ), contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = "Complete",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .align(Alignment.Center)
                                                    )
                                                }
                                            } else {
                                                // Empty circle for incomplete
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .border(
                                                            2.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape
                                                        )
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Expand/Collapse icon
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .rotate(if (isExpanded) 180f else 0f)
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Delete button
                                            if (phases.size > 1) {
                                                val deleteGradient = Brush.radialGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.error,
                                                        MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                    ), center = Offset(14f, 14f), radius = 14f
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(
                                                            brush = deleteGradient,
                                                            shape = CircleShape
                                                        )
                                                        .clickable {
                                                            phases.removeAt(index)
                                                            // Adjust expanded indices
                                                            val newExpanded =
                                                                expandedPhases.mapNotNull { i ->
                                                                    when {
                                                                        i < index -> i
                                                                        i > index -> i - 1
                                                                        else -> null
                                                                    }
                                                                }.toSet()
                                                            expandedPhases = newExpanded
                                                        }, contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.trash),
                                                        contentDescription = "Remove Phase",
                                                        tint = MaterialTheme.colorScheme.surface,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Divider below budget section
                                        //Spacer(modifier = Modifier.height(4.dp))
                                        Divider(
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(2.dp)
                                        )

                                        // Collapsible content
                                        if (isExpanded) {
                                            // Phase Name
                                            // Check for duplicate phase names (case-insensitive, ignoring empty names)
                                            // Check both within current project phases and all existing phases from other projects
                                            val currentPhaseName = phaseDraft.phaseName.trim()
                                            val isDuplicateInCurrentProject =
                                                currentPhaseName.isNotEmpty() && phases.mapIndexedNotNull { idx, phase ->
                                                    if (idx != index) phase.phaseName.trim()
                                                        .lowercase() else null
                                                }.contains(currentPhaseName.lowercase())

                                            val isDuplicateInExistingPhases =
                                                remember(currentPhaseName, allExistingPhases) {
                                                    currentPhaseName.isNotEmpty() && allExistingPhases.any { existingPhase ->
                                                        existingPhase.phaseName.trim()
                                                            .lowercase() == currentPhaseName.lowercase()
                                                    }
                                                }

                                            val isDuplicatePhaseName =
                                                isDuplicateInCurrentProject || isDuplicateInExistingPhases
                                            val hasError =
                                                fieldError == "phaseName_$index" || fieldError == "phaseNameDuplicate_$index" || isDuplicatePhaseName

                                            // Gradient backgrounds based on error state
                                            val backgroundGradient = if (hasError) {
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f), // Red opacity 0.1
                                                        MaterialTheme.colorScheme.error.copy(alpha = 0.05f) // Red opacity 0.05
                                                    ), start = Offset(0f, 0f), end = Offset(
                                                        Float.POSITIVE_INFINITY,
                                                        Float.POSITIVE_INFINITY
                                                    )
                                                )
                                            } else {
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), // Green opacity 0.15
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), // Blue opacity 0.15
                                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)  // Purple opacity 0.15
                                                    ), start = Offset(0f, 0f), end = Offset(
                                                        Float.POSITIVE_INFINITY,
                                                        Float.POSITIVE_INFINITY
                                                    )
                                                )
                                            }

                                            val borderGradient = if (hasError) {
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.error, // Red
                                                        MaterialTheme.colorScheme.error.copy(alpha = 0.6f) // Red opacity 0.6
                                                    ), start = Offset(0f, 0f), end = Offset(
                                                        Float.POSITIVE_INFINITY,
                                                        Float.POSITIVE_INFINITY
                                                    )
                                                )
                                            } else {
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), // Green opacity 0.4
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), // Blue opacity 0.4
                                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)  // Purple opacity 0.4
                                                    ), start = Offset(0f, 0f), end = Offset(
                                                        Float.POSITIVE_INFINITY,
                                                        Float.POSITIVE_INFINITY
                                                    )
                                                )
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .onGloballyPositioned { coordinates ->
                                                        fieldPositions["phaseName_$index"] =
                                                            coordinates.positionInParent().y
                                                    }) {
                                                // Label
                                                Text(
                                                    text = "Phase Name",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )

                                                // TextField with gradient background
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            brush = backgroundGradient,
                                                            shape = RoundedCornerShape(14.dp)
                                                        )
                                                        .border(
                                                            width = if (hasError) 2.dp else 1.5.dp,
                                                            brush = borderGradient,
                                                            shape = RoundedCornerShape(14.dp)
                                                        )
                                                ) {
                                                    OutlinedTextField(
                                                        value = phaseDraft.phaseName,
                                                        onValueChange = { newText ->
                                                            phases[index] =
                                                                phases[index].copy(phaseName = newText)
                                                            // Clear error when user starts typing
                                                            if (fieldError == "phaseName_$index" || fieldError == "phaseNameDuplicate_$index") {
                                                                fieldError = null
                                                            }
                                                        },
                                                        placeholder = {
                                                            Text(
                                                                "Enter phase name",
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(14.dp),
                                                        textStyle = androidx.compose.ui.text.TextStyle(
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        ),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedContainerColor = Color.Transparent,
                                                            unfocusedContainerColor = Color.Transparent,
                                                            focusedBorderColor = Color.Transparent,
                                                            unfocusedBorderColor = Color.Transparent,
                                                            errorBorderColor = Color.Transparent,
                                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    )
                                                }

                                                // Error message
                                                if (hasError) {
                                                    Row(
                                                        modifier = Modifier.padding(top = 4.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            4.dp
                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Warning,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = when {
                                                            fieldError == "phaseName_$index" -> "Phase name is required"
                                                            fieldError == "phaseNameDuplicate_$index" || isDuplicatePhaseName -> {
                                                                val duplicateName =
                                                                    currentPhaseName.ifEmpty { phaseDraft.phaseName }
                                                                "$duplicateName already exists in this project. Enter a unique phase name."
                                                            }

                                                            else -> ""
                                                        },
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.error,
                                                            fontWeight = FontWeight.Normal)
                                                    }
                                                }
                                            }

                                            // Timeline Section
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .onGloballyPositioned { coordinates ->
                                                        fieldPositions["startDate_$index"] =
                                                            coordinates.positionInParent().y
                                                    },
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Timeline Label
                                                Text(
                                                    text = "Timeline",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DateRange,
                                                        contentDescription = "Start Date",
                                                        modifier = Modifier.size(20.dp)
                                                    )

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Text(
                                                        text = "Start date",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }


                                                // Start Date Row
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            activePhaseIndexForStart = index
                                                            showStartDatePicker = true
                                                            // Clear error when user selects date
                                                            if (fieldError == "startDate_$index") {
                                                                fieldError = null
                                                            }
                                                        },
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    /*Icon(
                                                        Icons.Default.DateRange,
                                                        contentDescription = "Select Start Date",
                                                        tint = if (fieldError == "startDate_$index") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(20.dp)
                                                    )*/
                                                    Text(
                                                        text = "Select start date",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    // Date Box
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                MaterialTheme.colorScheme.surface,
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(
                                                                horizontal = 12.dp, vertical = 10.dp
                                                            ), contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(text = phaseDraft.start?.let {
                                                            val displayFormatter = SimpleDateFormat(
                                                                "MMM dd, yyyy",
                                                                Locale.getDefault()
                                                            )
                                                            displayFormatter.format(it)
                                                        } ?: "",
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (phaseDraft.start == null) Color(
                                                                0xFF8E8E93
                                                            ) else MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }

                                                // Error message for start date
                                                if (fieldError == "startDate_$index") {
                                                    Text(
                                                        text = if (plannedStartDate != null) {
                                                            "Phase start date must be on or after Planned Start Date (${
                                                                dateFormatter.format(
                                                                    plannedStartDate
                                                                )
                                                            })"
                                                        } else {
                                                            "Start date is required"
                                                        },
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.padding(
                                                            start = 28.dp, top = 4.dp
                                                        )
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DateRange,
                                                        contentDescription = "End Date",
                                                        modifier = Modifier.size(20.dp)
                                                    )

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Text(
                                                        text = "End date",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }


                                                // End Date Row
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .onGloballyPositioned { coordinates ->
                                                            fieldPositions["endDate_$index"] =
                                                                coordinates.positionInParent().y
                                                        }
                                                        .clickable {
                                                            activePhaseIndexForEnd = index
                                                            showEndDatePicker = true
                                                            // Clear error when user selects date
                                                            if (fieldError == "endDate_$index" || fieldError == "endDateBeforeStart_$index") {
                                                                fieldError = null
                                                            }
                                                        },
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    /*Icon(
                                                        Icons.Default.DateRange,
                                                        contentDescription = "Select End Date",
                                                        tint = if (fieldError == "endDate_$index" || fieldError == "endDateBeforeStart_$index") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )*/
                                                    Text(
                                                        text = "Select end date",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    // Date Box
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                MaterialTheme.colorScheme.surface,
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(
                                                                horizontal = 12.dp, vertical = 10.dp
                                                            ), contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(text = phaseDraft.end?.let {
                                                            val displayFormatter = SimpleDateFormat(
                                                                "MMM dd, yyyy",
                                                                Locale.getDefault()
                                                            )
                                                            displayFormatter.format(it)
                                                        } ?: "",
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (phaseDraft.end == null) Color(
                                                                0xFF8E8E93
                                                            ) else MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }

                                                // Error message for end date
                                                if (fieldError == "endDate_$index" || fieldError == "endDateBeforeStart_$index") {
                                                    Text(
                                                        text = when {
                                                            fieldError == "endDate_$index" -> "End date is required"
                                                            fieldError == "endDateBeforeStart_$index" -> "End date must be on or after start date"
                                                            else -> ""
                                                        },
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.padding(
                                                            start = 28.dp, top = 4.dp
                                                        )
                                                    )
                                                }
                                            }

                                            // Inline error if end date is before start date (handled by field error above)

                                            // Departments for this phase
                                            var showAddDeptDialog by remember(index) {
                                                mutableStateOf(
                                                    false
                                                )
                                            }
                                            var newDeptName by remember(index) { mutableStateOf("") }
                                            var newDeptContractorMode by remember(index) {
                                                mutableStateOf(
                                                    ContractorMode.LABOUR_ONLY
                                                )
                                            }
                                            var newDeptLineItems by remember(index) {
                                                mutableStateOf(
                                                    mutableListOf<LineItem>()
                                                )
                                            }
                                            var showAddPhaseSheet by remember(index) { mutableStateOf(false) }
                                            var newPhaseName by remember(index) { mutableStateOf("") }

                                            // Check for duplicate department names within the same phase (case-insensitive)
                                            val isDuplicateDepartmentName =
                                                remember(newDeptName, phaseDraft.departments) {
                                                    val currentDeptName = newDeptName.trim()
                                                    if (currentDeptName.isEmpty()) {
                                                        false
                                                    } else {
                                                        phaseDraft.departments.any { existingDept ->
                                                            existingDept.departmentName.trim()
                                                                .lowercase() == currentDeptName.lowercase()
                                                        }
                                                    }
                                                }
                                            Row {
                                                Text(
                                                    text = "Manager & Team for this phase are inherited from Project Team section",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Departments Section Header
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Department icon with blue circular background
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.primary, CircleShape
                                                        ), contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.GridOn,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.surface,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "DEPARTMENTS",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            // Department list
                                            if (phaseDraft.departments.isNotEmpty()) {
                                                phaseDraft.departments.forEachIndexed { deptIndex, dept ->
                                                    DepartmentBudgetItem(
                                                        department = dept,
                                                        departmentUserCounts = departmentUserCounts,
                                                        departmentApproverCounts = departmentApproverCounts,
                                                        onDepartmentNameChange = { newName: String ->
                                                            val updatedList =
                                                                phases[index].departments.toMutableList()
                                                            updatedList[deptIndex] =
                                                                updatedList[deptIndex].copy(
                                                                    departmentName = newName
                                                                )
                                                            phases[index] =
                                                                phases[index].copy(departments = updatedList)
                                                        },
                                                        onLineItemsChange = { updatedLineItems: List<LineItem> ->
                                                            val updatedList =
                                                                phases[index].departments.toMutableList()
                                                            updatedList[deptIndex] =
                                                                updatedList[deptIndex].copy(
                                                                    lineItems = updatedLineItems.toMutableList()
                                                                )
                                                            phases[index] =
                                                                phases[index].copy(departments = updatedList)
                                                        },
                                                        onContractorModeChange = { newMode: ContractorMode ->
                                                            val updatedList =
                                                                phases[index].departments.toMutableList()
                                                            updatedList[deptIndex] =
                                                                updatedList[deptIndex].copy(
                                                                    contractorMode = newMode
                                                                )
                                                            phases[index] =
                                                                phases[index].copy(departments = updatedList)
                                                        },
                                                        onContractorAmountChange = { newAmount: Double ->
                                                            val updatedList =
                                                                phases[index].departments.toMutableList()
                                                            updatedList[deptIndex] =
                                                                updatedList[deptIndex].copy(
                                                                    contractorAmount = newAmount
                                                                )
                                                            phases[index] =
                                                                phases[index].copy(departments = updatedList)
                                                        },
                                                        onRemove = {
                                                            // Prevent removing the last department from a phase
                                                            if (phases[index].departments.size == 1) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Cannot remove the last department. Each phase must have at least one department.",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            } else {
                                                                val newList =
                                                                    phases[index].departments.toMutableList()
                                                                newList.removeAt(deptIndex)
                                                                phases[index] =
                                                                    phases[index].copy(departments = newList)
                                                            }
                                                        },
                                                        onAddDepartment = {
                                                            showAddDeptDialog = true
                                                        })
                                                    // Add divider between departments (except after the last one)
                                                    if (deptIndex < phaseDraft.departments.size - 1) {
                                                        HorizontalDivider(
                                                            modifier = Modifier.padding(vertical = 8.dp),
                                                            color = MaterialTheme.colorScheme.outlineVariant,
                                                            thickness = 1.dp
                                                        )
                                                    }
                                                }

                                                // Divider after departments (only if there are departments)
                                                Divider(
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                    thickness = 1.dp,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }

                                            // Add Department Button (always visible, even when no departments)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showAddDeptDialog = true }
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Start,
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.primary, CircleShape
                                                        ), contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.surface
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Add Department",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            // Department Creation Bottom Sheet
                                            if (showAddDeptDialog) {
                                                AddDepartmentModalSheet(
                                                    phaseName = phaseDraft.phaseName,
                                                    departmentName = newDeptName,
                                                    onDepartmentNameChange = { newDeptName = it },
                                                    contractorMode = newDeptContractorMode,
                                                    onContractorModeChange = {
                                                        newDeptContractorMode = it
                                                    },
                                                    lineItems = newDeptLineItems,
                                                    onLineItemsChange = {
                                                        newDeptLineItems = it.toMutableList()
                                                    },
                                                    isDuplicateDepartmentName = isDuplicateDepartmentName,
                                                    onDismiss = {
                                                        showAddDeptDialog = false
                                                        newDeptName = ""
                                                        newDeptContractorMode =
                                                            ContractorMode.LABOUR_ONLY
                                                        newDeptLineItems = mutableListOf()
                                                    },
                                                    onCreate = { _, _, contractorAmount -> // approverPhone, userPhone - not used here
                                                        if (!isDuplicateDepartmentName && newDeptName.isNotBlank()) {
                                                            val newList =
                                                                phases[index].departments.toMutableList()
                                                            val newDept = DepartmentDraft(
                                                                departmentName = newDeptName.trim(),
                                                                contractorMode = newDeptContractorMode,
                                                                lineItems = newDeptLineItems.toMutableList(),
                                                                contractorAmount = contractorAmount
                                                            )
                                                            newList.add(newDept)
                                                            phases[index] =
                                                                phases[index].copy(departments = newList)
                                                            newDeptName = ""
                                                            newDeptContractorMode =
                                                                ContractorMode.LABOUR_ONLY
                                                            newDeptLineItems = mutableListOf()
                                                            showAddDeptDialog = false
                                                        }
                                                    })
                                            }

                                            // Add Phase button - shown after departments section of each phase
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                thickness = 1.dp,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showAddPhaseSheet = true }
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Start,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.secondary, CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.surface
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Add Phase",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }

                                            // Add Phase Bottom Sheet
                                            if (showAddPhaseSheet) {
                                                val addPhaseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                                                ModalBottomSheet(
                                                    onDismissRequest = {
                                                        showAddPhaseSheet = false
                                                        newPhaseName = ""
                                                    },
                                                    sheetState = addPhaseSheetState,
                                                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(24.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(36.dp)
                                                                    .background(
                                                                        MaterialTheme.colorScheme.secondary, CircleShape
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.Layers,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.surface,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Text(
                                                                text = "Add New Phase",
                                                                fontSize = 20.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(24.dp))
                                                        OutlinedTextField(
                                                            value = newPhaseName,
                                                            onValueChange = { newPhaseName = it },
                                                            label = { Text("Phase Name") },
                                                            placeholder = { Text("e.g. Foundation, Structure, Finishing") },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true,
                                                            shape = RoundedCornerShape(10.dp),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                            )
                                                        )
                                                        Spacer(modifier = Modifier.height(24.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                            OutlinedButton(
                                                                onClick = {
                                                                    showAddPhaseSheet = false
                                                                    newPhaseName = ""
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(10.dp)
                                                            ) {
                                                                Text("Cancel")
                                                            }
                                                            Button(
                                                                onClick = {
                                                                    if (newPhaseName.isNotBlank()) {
                                                                        phases.add(PhaseDraft(phaseName = newPhaseName.trim()))
                                                                        expandedPhases = expandedPhases + (phases.size - 1)
                                                                        newPhaseName = ""
                                                                        showAddPhaseSheet = false
                                                                    }
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(10.dp),
                                                                enabled = newPhaseName.isNotBlank()
                                                            ) {
                                                                Text("Add Phase")
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Collect unique department names used in all phases
                        val phaseDepartmentNames =
                            phases.flatMap { it.departments }.mapNotNull { it.departmentName }
                                .filter { it.isNotBlank() }.distinct()

                        // Departments that have at least 2 approvers/users for this customer
                        val filterDepartmentsforApprover = phaseDepartmentNames.filter { name ->
                            (departmentApproverCounts[name] ?: 0L) > 1L
                        }

                        val filterDepartmentsforUser = phaseDepartmentNames.filter { name ->
                            (departmentUserCounts[name] ?: 0L) > 1L
                        }

                        val DirectSaveUsersForDepartments = phaseDepartmentNames.filter { name ->
                            (departmentUserCounts[name] ?: 0L) == 1L
                        }

                        val DirectSaveApproversForDepartments = phaseDepartmentNames.filter { name ->
                            (departmentApproverCounts[name] ?: 0L) == 1L
                        }

                        // PROJECT MANAGER Section
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Account Manager",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "Required",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Manager dropdown
                                ExposedDropdownMenuBox(
                                    expanded = showManagerDropdown,
                                    onExpandedChange = { showManagerDropdown = it && availableManagers.isNotEmpty() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedManager?.name ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        isError = fieldError == "accountManager",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        placeholder = { Text("Select an Account Manager…") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showManagerDropdown)
                                        },
                                        supportingText = if (fieldError == "accountManager") {
                                            {
                                                Text(
                                                    text = "Please select an account manager",
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        } else null,
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (fieldError == "accountManager") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = if (fieldError == "accountManager") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    )

                                    ExposedDropdownMenu(
                                        expanded = showManagerDropdown,
                                        onDismissRequest = { showManagerDropdown = false }
                                    ) {
                                        if (availableManagers.isEmpty()) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "No managers available",
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                onClick = { showManagerDropdown = false }
                                            )
                                        } else {
                                            availableManagers.forEach { manager ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(32.dp)
                                                                    .background(
                                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                                        CircleShape
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = manager.name.firstOrNull()?.uppercase() ?: "M",
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = manager.name,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Text(
                                                                    text = manager.phone,
                                                                    fontSize = 12.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            if (selectedManager?.phone == manager.phone) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedManager = manager
                                                        showManagerDropdown = false
                                                    }
                                                )
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 8.dp),
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // TEAM ASSIGNMENT Section - iOS White Card

                        if (filterDepartmentsforApprover.size > 0 || filterDepartmentsforUser.size > 0) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                // 1. Calculate your counts and total based on your previous logic
                                val approverCountState = remember { mutableStateOf("0/${filterDepartmentsforApprover.size}") }
                                val userCountState = remember { mutableStateOf("0/${filterDepartmentsforUser.size}") }
                                var showUserAssignmentScreen by remember { mutableStateOf(false) }
                                var showApproverAssignmentScreen by remember { mutableStateOf(false) }


// 2. The UI Component
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {

                                        // --- BUTTON 1: USER ---
                                        if (filterDepartmentsforUser.size > 0) {
                                            Button(
                                                // Logic: toggle the 'User' filter
                                                onClick = {
                                                    showUserAssignmentScreen = true
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer, // Light Blue
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer    // Dark Blue Text
                                                ),
                                                elevation = ButtonDefaults.buttonElevation(0.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "User",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    // The Count Badge
                                                    Text(
                                                        text = userCountState.value,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }

                                        // --- BUTTON 2: APPROVER ---
                                        if (filterDepartmentsforApprover.size > 0) {
                                            Button(
                                                // Logic: toggle the 'Approver' filter
                                                onClick = {
                                                    showApproverAssignmentScreen = true
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer, // Light Green
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer    // Dark Green Text
                                                ),
                                                elevation = ButtonDefaults.buttonElevation(0.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Approver",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    // The Count Badge
                                                    Text(
                                                        text = approverCountState.value,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (showUserAssignmentScreen){
                                    Log.d("UsersNames","users names for passing $availableUsers")
                                    ModalBottomSheet(
                                        onDismissRequest = { showUserAssignmentScreen = false },
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    ) {
                                        // CALL THE COMPOSABLE FROM PREVIOUS ANSWER
                                        UserAssignmentSheet(
                                            assignmentCountString = userCountState, // Pass the MUTABLE state
                                            departments = filterDepartmentsforUser,
                                            users = availableUsers,
                                            initialAssignments = finalUserAssignments, // Pass previously assigned values
                                            onAssignmentComplete = { results ->
                                                finalUserAssignments = results
                                                showUserAssignmentScreen = false // Close sheet
                                            }
                                        )
                                    }
                                }
                                if (showApproverAssignmentScreen){
                                    ModalBottomSheet(
                                        onDismissRequest = { showApproverAssignmentScreen = false },
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    ) {
                                        ApproverAssignmentSheet(
                                            assignmentCountString = approverCountState,
                                            departments = filterDepartmentsforApprover,
                                            approvers = availableApprovers,
                                            initialAssignments = finalApproverAssignments, // Pass previously assigned values
                                            onAssignmentComplete = { results ->
                                                finalApproverAssignments = results
                                                showApproverAssignmentScreen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // Auto-assign users/approvers for departments with only 1 option
                        LaunchedEffect(DirectSaveUsersForDepartments, DirectSaveApproversForDepartments, availableUsers, availableApprovers) {
                            if (DirectSaveUsersForDepartments.isNotEmpty() || DirectSaveApproversForDepartments.isNotEmpty()) {
                                // Auto-assign users for departments with exactly 1 user
                                val updatedUserAssignments = finalUserAssignments.toMutableMap()
                                for (userDepartment in DirectSaveUsersForDepartments) {
                                    val usersForDepartment = availableUsers.filter { user ->
                                        user.department == userDepartment
                                    }
                                    if (usersForDepartment.isNotEmpty()) {
                                        // Store user phone (matching what assignment sheets expect)
                                        updatedUserAssignments[userDepartment] = usersForDepartment[0].phone
                                    }
                                }
                                finalUserAssignments = updatedUserAssignments

                                // Auto-assign approvers for departments with exactly 1 approver
                                val updatedApproverAssignments = finalApproverAssignments.toMutableMap()
                                for (approverDepartment in DirectSaveApproversForDepartments) {
                                    val approversForDepartment = availableApprovers.filter { approver ->
                                        approver.department == approverDepartment
                                    }
                                    if (approversForDepartment.isNotEmpty()) {
                                        // Store approver phone (matching what assignment sheets expect)
                                        updatedApproverAssignments[approverDepartment] = approversForDepartment[0].phone
                                    }
                                }
                                finalApproverAssignments = updatedApproverAssignments
                            }
                        }

                        // Removed old shared departments section; departments are now managed per phase

                        // Categories section removed as per user request

                        // Review Button - iOS Style (matching search field padding)
                        Button(
                            onClick = {
                                try {
                                    android.util.Log.d("NewProjectScreen", "🔍 Review button clicked")
                                    // Always allow clicking - validation will handle missing fields
                                    // Clear previous field errors
                                    fieldError = null
                                    fieldErrorPhaseIndex = null

                                    // Validate primary number is not blank (it is required)
                                    if (clientPrimaryNumber.isBlank()) {
                                        clientPrimaryNumberError = "Primary contact number is required"
                                    } else {
                                        val primaryDigits = clientPrimaryNumber.filter { it.isDigit() }
                                        if (primaryDigits.length < primaryCountryCode.minDigits) {
                                            clientPrimaryNumberError = "Must be ${primaryCountryCode.minDigits} digits for ${primaryCountryCode.name}"
                                        } else if (primaryDigits.length > primaryCountryCode.maxDigits) {
                                            clientPrimaryNumberError = "Cannot exceed ${primaryCountryCode.maxDigits} digits for ${primaryCountryCode.name}"
                                        }
                                    }

                                    // Block if phone fields have validation errors
                                    if (clientPrimaryNumberError != null || clientSecondaryNumberError != null) {
                                        android.widget.Toast.makeText(
                                            context,
                                            if (clientPrimaryNumber.isBlank()) "Primary contact number is required"
                                            else "Please fix the phone number errors before proceeding",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        return@Button
                                    }

                                    // Validate and set field errors (without approver and team members)
                                    val validationResult = validateFormAndSetErrors(
                                        projectName = projectName,
                                        description = description,
                                        client = client,
                                        location = location,
                                        currency = currency,
                                        selectedManager = selectedManager,
                                        selectedApprover = null, // Not required anymore
                                        selectedTeamMembers = emptyList(), // Not required anymore
                                        phases = phases,
                                        allExistingPhases = allExistingPhases,
                                        plannedStartDate = plannedStartDate,
                                        finalUserAssignments = finalUserAssignments,
                                        finalApproverAssignments = finalApproverAssignments,
                                        departmentUserCounts = departmentUserCounts,
                                        departmentApproverCounts = departmentApproverCounts
                                    )

                                    android.util.Log.d("NewProjectScreen", "🔍 Validation result: isValid=${validationResult.isValid}, error=${validationResult.fieldError}, phaseIndex=${validationResult.phaseIndex}")

                                    if (validationResult.isValid) {
                                        // All fields are valid, navigate to Review screen
                                        android.util.Log.d("NewProjectScreen", "✅ Validation passed, navigating to review")
                                        android.util.Log.d("NewProjectScreen", "📊 Project: $projectName, Phases: ${phases.size}, User Assignments: ${finalUserAssignments.size}, Approver Assignments: ${finalApproverAssignments.size}")
                                        
                                        // Verify we have all required data
                                        if (phases.isEmpty()) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Please add at least one phase",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                            return@Button
                                        }
                                        
                                        // Validate department-specific user/approver assignments
                                        val phaseDepartmentNames = phases.flatMap { it.departments }
                                            .mapNotNull { it.departmentName }
                                            .filter { it.isNotBlank() }
                                            .distinct()
                                        
                                        // Check departments that need user assignments
                                        val departmentsNeedingUsers = phaseDepartmentNames.filter { dept ->
                                            (departmentUserCounts[dept] ?: 0L) > 1L
                                        }
                                        val missingUserAssignments = departmentsNeedingUsers.filter { dept ->
                                            !finalUserAssignments.containsKey(dept) || finalUserAssignments[dept].isNullOrBlank()
                                        }
                                        
                                        // Check departments that need approver assignments
                                        val departmentsNeedingApprovers = phaseDepartmentNames.filter { dept ->
                                            (departmentApproverCounts[dept] ?: 0L) > 1L
                                        }
                                        val missingApproverAssignments = departmentsNeedingApprovers.filter { dept ->
                                            !finalApproverAssignments.containsKey(dept) || finalApproverAssignments[dept].isNullOrBlank()
                                        }
                                        
                                        if (missingUserAssignments.isNotEmpty()) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Please assign users for departments: ${missingUserAssignments.joinToString(", ")}",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                            return@Button
                                        }
                                        
                                        if (missingApproverAssignments.isNotEmpty()) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Please assign approvers for departments: ${missingApproverAssignments.joinToString(", ")}",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                            return@Button
                                        }
                                        
                                        // Show toast to confirm button click
                                        android.widget.Toast.makeText(
                                            context,
                                            "Navigating to review...",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        
                                        scope.launch {
                                            try {
                                                val isDuplicateProjectName =
                                                    viewModel.isProjectNameAlreadyUsedForCurrentCustomer(
                                                        projectName = projectName,
                                                        excludeProjectId = projectIdForResubmission
                                                    )
                                                if (isDuplicateProjectName) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Project name already exists. Please select a different name.",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                    return@launch
                                                }

                                                onNavigateToReview(
                                                    projectName,
                                                    description,
                                                    client,
                                                    clientPrimaryNumber.takeIf { it.isNotBlank() },
                                                    clientSecondaryNumber.takeIf { it.isNotBlank() },
                                                    location,
                                                    currency,
                                                    plannedStartDate,
                                                    null,
                                                    null,
                                                    phases,
                                                    selectedManager,
                                                    emptyList(),
                                                    finalUserAssignments,
                                                    finalApproverAssignments
                                                )
                                                android.util.Log.d("NewProjectScreen", "✅ Navigation callback executed successfully")
                                            } catch (e: Exception) {
                                                android.util.Log.e("NewProjectScreen", "❌ Error in navigation callback: ${e.message}", e)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Navigation error: ${e.message}",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    } else {
                                        // Set field error to trigger auto-scroll
                                        android.util.Log.d("NewProjectScreen", "❌ Validation failed: ${validationResult.fieldError}")
                                        val errorMessage = when (validationResult.fieldError) {
                                            "projectName" -> "Please enter project name"
                                            "description" -> "Please enter description"
                                            "client" -> "Please enter client name"
                                            "location" -> "Please enter location"
                                            "currency" -> "Please select currency"
                                            "accountManager" -> "Please select an account manager"
                                            "phases" -> "Please add at least one phase"
                                            "departmentUsers" -> "Please assign users for all departments"
                                            "departmentApprovers" -> "Please assign approvers for all departments"
                                            else -> "Please complete all required fields"
                                        }
                                        android.widget.Toast.makeText(
                                            context,
                                            errorMessage,
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        fieldError = validationResult.fieldError
                                        fieldErrorPhaseIndex = validationResult.phaseIndex
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("NewProjectScreen", "❌ Error in review button click: ${e.message}", e)
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            enabled = true, // Always enabled - validation handles errors
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp), // Add bottom padding for better visibility
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.reviewproject),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Review Project",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.surface
                                )
                            }
                        }

                        // Error Message
                        error?.let { errorMessage ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage, color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        if (isResubmissionLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    // Planned Start Date Picker is now inline/floating (no modal dialog)

    // Auto-populate phase dates when plannedStartDate or phases change and autoSelect is enabled
    LaunchedEffect(plannedStartDate, autoSelectPhaseDates, phases.size) {
        try {
            if (!autoSelectPhaseDates) return@LaunchedEffect
            val planned = plannedStartDate ?: return@LaunchedEffect

            // Start from planned date for first phase
            val cal = Calendar.getInstance()
            cal.time = planned
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            for (i in phases.indices) {
                val start = if (i == 0) {
                    cal.time
                } else {
                    // previous phase end + 1 day
                    val prevEnd = phases[i - 1].end ?: break
                    val c = Calendar.getInstance()
                    c.time = prevEnd
                    c.add(Calendar.DAY_OF_MONTH, 1)
                    c.time
                }

                // end = start + 1 month
                val endCal = Calendar.getInstance()
                endCal.time = start
                endCal.add(Calendar.MONTH, 1)
                val end = endCal.time

                // Only overwrite if field is null or autoSelect toggled on
                phases[i] = phases[i].copy(start = start, end = end)
            }
        } catch (e: Exception) {
            android.util.Log.e("NewProjectScreen", "Error auto-populating phase dates: ${e.message}", e)
        }
    }

    // Start Date Picker (per-phase)
    if (showStartDatePicker) {
        activePhaseIndexForStart?.let { idx ->
            // Get planned start date as minimum date (but never earlier than today)
            val calToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStartMillis = calToday.timeInMillis
            val minDateMillis = maxOf(plannedStartDate?.time ?: todayStartMillis, todayStartMillis)

            // Update date picker state with minimum date
            LaunchedEffect(showStartDatePicker, plannedStartDate) {
                if (showStartDatePicker) {
                    // Set initial date to current phase start date or planned start date
                    val initialDate = phases[idx].start?.time ?: minDateMillis
                    startDatePickerState.selectedDateMillis = maxOf(initialDate, minDateMillis)
                }
            }

            DatePickerDialog(
                onDateSelected = { selectedDate ->
                // Validate: Phase start date must be >= Planned start date
                val currentPlannedStartDate = plannedStartDate
                if (currentPlannedStartDate != null && selectedDate.time < currentPlannedStartDate.time) {
                    // Normalize dates to 12:00 AM for comparison
                    val calendarSelected = java.util.Calendar.getInstance().apply {
                        time = selectedDate
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.time

                    val calendarPlanned = java.util.Calendar.getInstance().apply {
                        time = currentPlannedStartDate
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.time

                    if (calendarSelected.time < calendarPlanned.time) {
                        Toast.makeText(
                            context,
                            "Phase start date must be on or after Planned Start Date (${
                                dateFormatter.format(
                                    currentPlannedStartDate
                                )
                            })",
                            Toast.LENGTH_LONG
                        ).show()
                        // Set error for this phase
                        fieldError = "startDate_$idx"
                        fieldErrorPhaseIndex = idx
                        showStartDatePicker = false
                        return@DatePickerDialog
                    }
                }

                // Date is valid, update phase
                phases[idx] = phases[idx].copy(start = selectedDate)
                // Clear error if it was set
                if (fieldError == "startDate_$idx") {
                    fieldError = null
                    fieldErrorPhaseIndex = null
                }
                showStartDatePicker = false
            },
                onDismiss = { showStartDatePicker = false },
                datePickerState = startDatePickerState,
                title = "Select Start Date",
                minimumDateMillis = minDateMillis
            )
        }
    }

    // End Date Picker (per-phase)
    if (showEndDatePicker) {
        activePhaseIndexForEnd?.let { idx ->
            val minEndMillis = phases[idx].start?.time ?: System.currentTimeMillis()
            // Ensure initial selection is not earlier than min
            val initialEndMillis = maxOf(endDatePickerState.selectedDateMillis ?: 0L, minEndMillis)
            endDatePickerState.selectedDateMillis = initialEndMillis

            DatePickerDialog(
                onDateSelected = { selectedDate ->
                    activePhaseIndexForEnd?.let { idx2 ->
                        val start = phases[idx2].start
                        if (start != null && selectedDate.time < start.time) {
                            Toast.makeText(
                                context,
                                "End Planned date must be on or after Start Planned date",
                                Toast.LENGTH_SHORT
                            ).show()
                            fieldError = "endDateBeforeStart_$idx2"
                            fieldErrorPhaseIndex = idx2
                        } else {
                            phases[idx2] = phases[idx2].copy(end = selectedDate)
                            if (fieldError == "endDateBeforeStart_$idx2") {
                                fieldError = null
                                fieldErrorPhaseIndex = null
                            }
                        }
                    }
                    showEndDatePicker = false
                },
                onDismiss = { showEndDatePicker = false },
                datePickerState = endDatePickerState,
                title = "Select End Date",
                minimumDateMillis = minEndMillis
            )
        }
    }
}


@Composable
fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TeamMemberChip(
    user: User, onRemove: () -> Unit
) {
    val chipColors = listOf(
        MaterialTheme.colorScheme.tertiaryContainer, // Light Orange
        MaterialTheme.colorScheme.tertiaryContainer, // Light Purple
        MaterialTheme.colorScheme.primaryContainer, // Light Green
        MaterialTheme.colorScheme.secondaryContainer, // Light Blue
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )

    val chipColor = chipColors[user.hashCode().let { if (it < 0) -it else it } % chipColors.size]

    Card(
        modifier = Modifier.padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = chipColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = user.name.ifEmpty { "Unknown User" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onRemove, modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    datePickerState: DatePickerState,
    title: String,
    minimumDateMillis: Long? = null
) {
    // Track the millis that were already shown when the dialog opened so that
    // only a NEW tap (change) triggers auto-select-and-close.
    val initialMillis = remember { datePickerState.selectedDateMillis }

    // Auto-select and close when the user taps a different date cell.
    LaunchedEffect(Unit) {
        snapshotFlow { datePickerState.selectedDateMillis }
            .collect { millis ->
                if (millis != null && millis != initialMillis) {
                    // Validate minimum date if set
                    if (minimumDateMillis != null && millis < minimumDateMillis) {
                        return@collect
                    }
                    onDateSelected(Date(millis))
                }
            }
    }

    androidx.compose.material3.DatePickerDialog(onDismissRequest = onDismiss, confirmButton = {
        TextButton(
            onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    // Validate minimum date if set
                    if (minimumDateMillis != null && millis < minimumDateMillis) {
                        // Don't allow selection of date before minimum
                        return@TextButton
                    }
                    onDateSelected(Date(millis))
                }
            }) {
            Text("OK")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    }) {
        Column {
            // Show warning if minimum date is set
            if (minimumDateMillis != null) {
                val minimumDate = Date(minimumDateMillis)
                val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                Text(
                    text = "Please select a date on or after ${dateFormatter.format(minimumDate)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            DatePicker(
                state = datePickerState, modifier = Modifier.padding(16.dp), title = {
                    Text(
                        text = title, modifier = Modifier.padding(16.dp)
                    )
                }, showModeToggle = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdown(
    label: String,
    items: List<User>,
    selectedItem: User?,
    onItemSelected: (User?) -> Unit,
    itemText: (User) -> String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    availableCount: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded, onExpandedChange = onExpandedChange
            ) {
                OutlinedTextField(
                    value = selectedItem?.let { itemText(it) } ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(label) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = availableCount,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    // Selected item is now displayed separately above
                }

                ExposedDropdownMenu(
                    expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                    items.forEach { item ->
                        DropdownMenuItem(text = {
                            Column {
                                Text(
                                    text = item.name, fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = item.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }, onClick = {
                            onItemSelected(item)
                            onExpandedChange(false)
                        }, leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "User",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDropdown(
    label: String,
    items: List<User>,
    selectedItems: List<User>,
    onItemsSelected: (List<User>) -> Unit,
    itemText: (User) -> String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    availableCount: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded, onExpandedChange = onExpandedChange
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(label) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = availableCount,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    // Selected items are now displayed separately above
                }

                // Note: Selected items are now displayed separately above the dropdown

                ExposedDropdownMenu(
                    expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                    items.forEach { item ->
                        DropdownMenuItem(text = {
                            Column {
                                Text(
                                    text = item.name, fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = item.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }, onClick = {
                            // Add item to existing selectedItems if not already present
                            if (!selectedItems.contains(item)) {
                                onItemsSelected(selectedItems + item)
                            }
                            onExpandedChange(false)
                        }, leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "User",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentBudgetItem(
    department: DepartmentDraft,
    onDepartmentNameChange: (String) -> Unit,
    onLineItemsChange: (List<LineItem>) -> Unit,
    onContractorModeChange: (ContractorMode) -> Unit,
    onRemove: () -> Unit,
    onContractorAmountChange: (Double) -> Unit = {},
    onAddDepartment: () -> Unit = {},
    departmentUserCounts: Map<String, Long> = emptyMap(),
    departmentApproverCounts: Map<String, Long> = emptyMap()
) {
    var isExpanded by remember {
        mutableStateOf(
            department.lineItems.isNotEmpty() ||
                department.contractorAmount > 0.0 ||
                department.contractorMode != ContractorMode.LABOUR_ONLY
        )
    }
    var departmentName by remember { mutableStateOf(department.departmentName) }

    // Dropdown state
    var searchQuery by remember { mutableStateOf("") }
    var isDeptDropdownExpanded by remember { mutableStateOf(false) }

    val allDepartments = remember(departmentUserCounts, departmentApproverCounts) {
        (departmentUserCounts.keys + departmentApproverCounts.keys).distinct().sorted()
    }
    val enabledDepartments = remember(allDepartments, departmentUserCounts, departmentApproverCounts) {
        allDepartments.filter { dept ->
            (departmentUserCounts[dept] ?: 0L) > 0 && (departmentApproverCounts[dept] ?: 0L) > 0
        }
    }
    val disabledDepartments = remember(allDepartments, departmentUserCounts, departmentApproverCounts) {
        allDepartments.filter { dept ->
            val u = departmentUserCounts[dept] ?: 0L
            val a = departmentApproverCounts[dept] ?: 0L
            (u > 0 && a == 0L) || (u == 0L && a > 0)
        }
    }
    val filteredEnabled = remember(enabledDepartments, searchQuery) {
        if (searchQuery.isBlank()) enabledDepartments
        else enabledDepartments.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    val filteredDisabled = remember(disabledDepartments, searchQuery) {
        if (searchQuery.isBlank()) disabledDepartments
        else disabledDepartments.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    val isCustomDepartmentName = remember(departmentName, allDepartments) {
        departmentName.isNotBlank() && !allDepartments.contains(departmentName)
    }
    val isSelectedDepartmentDisabled = remember(departmentName, departmentUserCounts, departmentApproverCounts) {
        if (departmentName.isBlank()) false
        else {
            val u = departmentUserCounts[departmentName] ?: 0L
            val a = departmentApproverCounts[departmentName] ?: 0L
            (u > 0 && a == 0L) || (u == 0L && a > 0)
        }
    }
    var showAddLineItemSheet by remember { mutableStateOf(false) }
    var editingLineItemIndex by remember { mutableStateOf<Int?>(null) }
    var contractorAmountText by remember { mutableStateOf(if (department.contractorAmount > 0.0) department.contractorAmount.toString() else "") }

    // Update local state when department changes
    LaunchedEffect(department.departmentName) {
        departmentName = department.departmentName
    }

    // Update contractor amount when department changes
    LaunchedEffect(department.contractorAmount) {
        if (department.contractorAmount > 0.0) {
            contractorAmountText = department.contractorAmount.toString()
        }
    }

    LaunchedEffect(
        department.lineItems.size,
        department.contractorAmount,
        department.contractorMode
    ) {
        if (department.lineItems.isNotEmpty() ||
            department.contractorAmount > 0.0 ||
            department.contractorMode != ContractorMode.LABOUR_ONLY
        ) {
            isExpanded = true
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        //shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(0.dp)
        ) {
            // Department Name and Budget Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left side - Department Name Input
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "DEPARTMENT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)/*.background(
                                    brush = deleteGradient,
                                    shape = CircleShape
                                )*/.clickable { onRemove() }, contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.trash1),
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = isDeptDropdownExpanded,
                        onExpandedChange = { isDeptDropdownExpanded = !isDeptDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = departmentName,
                            onValueChange = { newValue ->
                                searchQuery = newValue
                                departmentName = newValue
                                onDepartmentNameChange(newValue)
                                isDeptDropdownExpanded = true
                            },
                            placeholder = { Text("Search department", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true,
                            isError = isCustomDepartmentName || isSelectedDepartmentDisabled,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFFE5E5EA),
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isDeptDropdownExpanded,
                            onDismissRequest = { isDeptDropdownExpanded = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            if (filteredEnabled.isNotEmpty()) {
                                filteredEnabled.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            departmentName = dept
                                            searchQuery = dept
                                            onDepartmentNameChange(dept)
                                            isDeptDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                            if (filteredDisabled.isNotEmpty()) {
                                if (filteredEnabled.isNotEmpty()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                                filteredDisabled.forEach { dept ->
                                    val needsApprover = (departmentApproverCounts[dept] ?: 0L) == 0L
                                    val needsUser = (departmentUserCounts[dept] ?: 0L) == 0L
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = dept,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = if (needsUser && needsApprover) "Needs approver and User"
                                                    else if (needsApprover) "Needs approver"
                                                    else "Needs user",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        },
                                        onClick = {
                                            departmentName = dept
                                            searchQuery = dept
                                            onDepartmentNameChange(dept)
                                            isDeptDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                            if (filteredEnabled.isEmpty() && filteredDisabled.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "No departments found",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {},
                                    enabled = false
                                )
                            }
                        }
                    }
                }

                // Right side - Budget, Delete, Expand
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 1.dp)
                    ) {
                        // BUDGET label and Delete icon on same row
                        Row(
                            // 1. Important: The Row must fill the width for the spacer to work
                            // modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically

                        ) {
                            // 2. This pushes everything after it to the far right
                            //Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "BUDGET",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = FormatUtils.formatCurrency(department.totalBudget),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary, // Lighter green color
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (isExpanded) 180f else 0f)
                        )
                    }
                }
            }

            // Expanded Content - Contractor Mode and Line Items
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // Contractor Mode Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "CONTRACTOR MODE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContractorModeCard(
                            title = "Labour-Only",
                            isSelected = department.contractorMode == ContractorMode.LABOUR_ONLY,
                            onClick = { onContractorModeChange(ContractorMode.LABOUR_ONLY) },
                            modifier = Modifier.weight(1f)
                        )
                        ContractorModeCard(
                            title = "Self Execution",
                            isSelected = department.contractorMode == ContractorMode.SELF_EXECUTION,
                            onClick = { onContractorModeChange(ContractorMode.SELF_EXECUTION) },
                            modifier = Modifier.weight(1f)
                        )
                        ContractorModeCard(
                            title = "Turnkey",
                            isSelected = department.contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY,
                            onClick = { onContractorModeChange(ContractorMode.TURNKEY_AMOUNT_ONLY) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Contractor Amount Input - for Labour-Only and Turnkey modes
                    if (department.contractorMode == ContractorMode.LABOUR_ONLY || department.contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY) {
                        OutlinedTextField(
                            value = contractorAmountText,
                            onValueChange = { newValue ->
                                contractorAmountText = newValue.filter { c -> c.isDigit() || c == '.' }
                                onContractorAmountChange(contractorAmountText.toDoubleOrNull() ?: 0.0)
                            },
                            label = { Text(if (department.contractorMode == ContractorMode.TURNKEY_AMOUNT_ONLY) "Turnkey" else "Contractor Amount") },
                            placeholder = { Text("Enter amount") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Line Items Section (applicable for Labour-Only and Self Execution, not for Turnkey)
                if (department.contractorMode == ContractorMode.LABOUR_ONLY || department.contractorMode == ContractorMode.SELF_EXECUTION) Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Line Item icon with blue circular background
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary, CircleShape
                                    ), contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ViewList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = "ITEMS",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "sum must equal Department Budget",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Line Items List
                    department.lineItems.forEachIndexed { itemIndex, lineItem ->
                        LineItemCard(
                            lineItem = lineItem,
                            contractorMode = department.contractorMode,
                            onLineItemChange = { updatedItem: LineItem ->
                                val updatedList = department.lineItems.toMutableList()
                                updatedList[itemIndex] = updatedItem
                                onLineItemsChange(updatedList)
                            },
                            onDelete = {
                                val updatedList = department.lineItems.toMutableList()
                                updatedList.removeAt(itemIndex)
                                onLineItemsChange(updatedList)
                            },
                            onEdit = {
                                editingLineItemIndex = itemIndex
                                showAddLineItemSheet = true
                            })
                    }

                    // Add Line Item Button (Orange)
                    Button(
                        onClick = {
                            editingLineItemIndex = null
                            showAddLineItemSheet = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                            contentColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Add Line Item",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Total Section (Σ calculation)
                    if (department.lineItems.isNotEmpty()) {
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Sigma symbol (Σ)
                                Text(
                                    text = "Σ",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Total Items",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = FormatUtils.formatCurrency(department.lineItems.sumOf { it.total }),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Line Item Sheet
    if (showAddLineItemSheet) {
        AddLineItemModalSheet(
            lineItem = editingLineItemIndex?.let { department.lineItems.getOrNull(it) } ?: LineItem(),
            contractorMode = department.contractorMode,
            onDismiss = {
                showAddLineItemSheet = false
                editingLineItemIndex = null
            },
            onAdd = { newLineItem ->
                val updatedList = department.lineItems.toMutableList()
                if (editingLineItemIndex != null && editingLineItemIndex!! < updatedList.size) {
                    updatedList[editingLineItemIndex!!] = newLineItem
                } else {
                    updatedList.add(newLineItem)
                }
                onLineItemsChange(updatedList)
                showAddLineItemSheet = false
                editingLineItemIndex = null
            },
            isEditMode = editingLineItemIndex != null
        )
    }
}

@Composable
fun ContractorModeCard(
    title: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        ),
        border = if (isSelected) BorderStroke(
            width = 1.dp, color = MaterialTheme.colorScheme.outline
        ) else BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.36f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LineItemCard(
    lineItem: LineItem,
    contractorMode: ContractorMode,
    onLineItemChange: (LineItem) -> Unit,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                //color = Color.LightGray,
                color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        // shape = RoundedCornerShape(0.dp),
        //elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // First row: Edit icon, Item Type and Item Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            CircleShape
                        )
                        .clickable { showEditDialog = true }, contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.tertiary, // Dark orange/brown pencil icon
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Item Type and Item Name on same line
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lineItem.itemType.ifEmpty { "Line Item" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "·",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = lineItem.item.ifEmpty { "Item" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (lineItem.spec.isNotEmpty()) {
                            Text(
                                text = "·",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = lineItem.spec,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Second row: Qty/Unit, Total Price, and Delete Icon on same line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Format quantity and unit price with middle dot and space after ₹
                val quantityText = if (lineItem.quantity % 1.0 == 0.0) {
                    lineItem.quantity.toInt().toString()
                } else {
                    String.format(Locale.US, "%.1f", lineItem.quantity)
                }
                val unitPriceFormatted = String.format(Locale.US, "%.2f", lineItem.unitPrice)

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(12.dp)) // Space for edit icon
                    Text(
                        text = "Qty: $quantityText · Unit: ₹ $unitPriceFormatted",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right side - Total and Delete
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Green Total Box
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = FormatUtils.formatCurrency(lineItem.total),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val deleteGradient = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        ), center = Offset(14f, 14f), radius = 14f
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                brush = deleteGradient, shape = CircleShape
                            )
                            .clickable { onDelete() }, contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.trash),
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog) {
        AddLineItemModalSheet(
            lineItem = lineItem,
            contractorMode = contractorMode,
            isEditMode = true,
            onDismiss = { showEditDialog = false },
            onAdd = { updatedItem ->
                onLineItemChange(updatedItem)
                showEditDialog = false
            })
    }
}

@Composable
fun CategoryItem(
    category: String, onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.List,
                contentDescription = "Category",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = category, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            val deleteGradient = Brush.radialGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.error,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                ), center = Offset(14f, 14f), radius = 14f
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        brush = deleteGradient, shape = CircleShape
                    )
                    .clickable { onRemove() }, contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.trash),
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun BudgetSummaryCard(
    totalBudget: Double, totalAllocated: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Budget Summary",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            val remainingBudget = totalBudget - totalAllocated

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Allocated:")
                Text(
                    text = formatter.format(totalAllocated), fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Budget:")
                Text(
                    text = formatter.format(totalBudget), fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Remaining Budget:", fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatter.format(remainingBudget),
                    fontWeight = FontWeight.Bold,
                    color = if (remainingBudget >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            // Show warning if allocated exceeds budget
            if (totalAllocated > totalBudget) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Allocated amount exceeds total budget!",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (remainingBudget > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "₹${formatter.format(remainingBudget)} available for allocation",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

fun isFormValidForPhases(
    projectName: String,
    selectedApprover: User?,
    selectedTeamMembers: List<User>,
    phases: List<PhaseDraft>
): Boolean {
    if (projectName.isEmpty()) return false
    if (selectedApprover == null) return false
    if (selectedTeamMembers.isEmpty()) return false
    if (phases.isEmpty()) return false
    // validate phase dates (both start and end are required; end must be >= start)
    phases.forEach { p ->
        val start = p.start ?: return false
        val end = p.end ?: return false // End date is now required
        if (end.time < start.time) return false
        // Each phase must have at least one department
        if (p.departments.isEmpty()) return false
    }
    return true
}

fun getPhaseFormValidationError(
    projectName: String,
    selectedApprover: User?,
    selectedTeamMembers: List<User>,
    phases: List<PhaseDraft>
): String {
    if (projectName.isEmpty()) return "Project name is required"
    if (selectedApprover == null) return "Please select a project manager (approver)"
    if (selectedTeamMembers.isEmpty()) return "Please select at least one team member"
    if (phases.isEmpty()) return "Please add at least one phase"
    // Ensure every phase has both start and end dates, and end >= start
    phases.forEachIndexed { idx, p ->
        val phaseNumber = idx + 1
        if (p.start == null) {
            return "Phase $phaseNumber requires a Start Date"
        }
        if (p.end == null) {
            return "Phase $phaseNumber requires an End Date"
        }
        // End date must be >= start date
        val startMillis = p.start!!.time
        val endMillis = p.end!!.time
        if (endMillis < startMillis) {
            return "Phase $phaseNumber: End Date must be on or after Start Date"
        }
        // Each phase must have at least one department
        if (p.departments.isEmpty()) {
            return "Phase $phaseNumber must have at least one department"
        }
    }
    return ""
}

// Validation result data class
data class ValidationResult(
    val isValid: Boolean, val fieldError: String? = null, val phaseIndex: Int? = null
)

// Validate form and set field errors for auto-scroll
fun validateFormAndSetErrors(
    projectName: String,
    description: String,
    client: String,
    location: String,
    currency: String,
    selectedManager: User?,
    selectedApprover: User?,
    selectedTeamMembers: List<User>,
    phases: List<PhaseDraft>,
    allExistingPhases: List<com.cubiquitous.tracura.model.Phase> = emptyList(),
    plannedStartDate: Date? = null,
    finalUserAssignments: Map<String, String> = emptyMap(),
    finalApproverAssignments: Map<String, String> = emptyMap(),
    departmentUserCounts: Map<String, Long> = emptyMap(),
    departmentApproverCounts: Map<String, Long> = emptyMap()
): ValidationResult {
    // Check project name
    if (projectName.isEmpty()) {
        return ValidationResult(false, "projectName", null)
    }

    // Check description
    if (description.isEmpty()) {
        return ValidationResult(false, "description", null)
    }

    // Check description word count (max 150 words)
    val descriptionWords = description.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    if (descriptionWords.size > 150) {
        return ValidationResult(false, "descriptionWordLimit", null)
    }

    // Check client
    if (client.isEmpty()) {
        return ValidationResult(false, "client", null)
    }

    // Check location
    if (location.isEmpty()) {
        return ValidationResult(false, "location", null)
    }

    // Check currency
    if (currency.isEmpty()) {
        return ValidationResult(false, "currency", null)
    }

    // Check account manager
    if (selectedManager == null) {
        return ValidationResult(false, "accountManager", null)
    }

    // Note: Approver and team members are no longer required - using department-specific assignments instead

    // Check phases
    // Validate planned start date is not earlier than today (if provided)
    if (plannedStartDate != null) {
        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calToday.time
        if (plannedStartDate.before(todayStart)) {
            return ValidationResult(false, "plannedStartDate", null)
        }
    }

    // Check phases
    if (phases.isEmpty()) {
        return ValidationResult(false, "phases", null)
    }

    // Validate each phase
    phases.forEachIndexed { idx, phase ->
        // Check phase name
        if (phase.phaseName.isEmpty()) {
            return ValidationResult(false, "phaseName_$idx", idx)
        }

        // Check for duplicate phase names (case-insensitive)
        // Check both within current project phases and all existing phases from other projects
        val currentPhaseName = phase.phaseName.trim().lowercase()

        // Check duplicates within current project
        val duplicateIndex = phases.mapIndexedNotNull { index, p ->
            if (index != idx && p.phaseName.trim().lowercase() == currentPhaseName) index else null
        }.firstOrNull()

        if (duplicateIndex != null) {
            return ValidationResult(false, "phaseNameDuplicate_$idx", idx)
        }

        // Check duplicates against all existing phases from other projects
        val isDuplicateInExistingPhases = allExistingPhases.any { existingPhase ->
            existingPhase.phaseName.trim().lowercase() == currentPhaseName
        }

        if (isDuplicateInExistingPhases) {
            return ValidationResult(false, "phaseNameDuplicate_$idx", idx)
        }

        // Check start date
        if (phase.start == null) {
            return ValidationResult(false, "startDate_$idx", idx)
        }

        // Check phase start date is >= planned start date
        if (plannedStartDate != null) {
            // Normalize dates to 12:00 AM for comparison
            val calendarPhaseStart = java.util.Calendar.getInstance().apply {
                time = phase.start!!
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time

            val calendarPlanned = java.util.Calendar.getInstance().apply {
                time = plannedStartDate
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time

            if (calendarPhaseStart.time < calendarPlanned.time) {
                return ValidationResult(false, "startDate_$idx", idx)
            }
        }

        // Check end date
        if (phase.end == null) {
            return ValidationResult(false, "endDate_$idx", idx)
        }

        // Check end date is after start date
        if (phase.end!!.time < phase.start!!.time) {
            return ValidationResult(false, "endDateBeforeStart_$idx", idx)
        }

        // Check departments
        if (phase.departments.isEmpty()) {
            return ValidationResult(false, "departments_$idx", idx)
        }

        // Check for duplicate department names within the same phase (case-insensitive)
        val departmentNames = phase.departments.map { it.departmentName.trim().lowercase() }
        val uniqueDepartmentNames = departmentNames.toSet()
        if (departmentNames.size != uniqueDepartmentNames.size) {
            return ValidationResult(false, "departmentsDuplicate_$idx", idx)
        }
    }

    // Validate department-specific user/approver assignments
    val phaseDepartmentNames = phases.flatMap { it.departments }
        .mapNotNull { it.departmentName }
        .filter { it.isNotBlank() }
        .distinct()
    
    // Check departments that need user assignments (> 1 user available)
    val departmentsNeedingUsers = phaseDepartmentNames.filter { dept ->
        (departmentUserCounts[dept] ?: 0L) > 1L
    }
    val missingUserAssignments = departmentsNeedingUsers.filter { dept ->
        !finalUserAssignments.containsKey(dept) || finalUserAssignments[dept].isNullOrBlank()
    }
    
    if (missingUserAssignments.isNotEmpty()) {
        return ValidationResult(false, "departmentUsers", null)
    }
    
    // Check departments that need approver assignments (> 1 approver available)
    val departmentsNeedingApprovers = phaseDepartmentNames.filter { dept ->
        (departmentApproverCounts[dept] ?: 0L) > 1L
    }
    val missingApproverAssignments = departmentsNeedingApprovers.filter { dept ->
        !finalApproverAssignments.containsKey(dept) || finalApproverAssignments[dept].isNullOrBlank()
    }
    
    if (missingApproverAssignments.isNotEmpty()) {
        return ValidationResult(false, "departmentApprovers", null)
    }

    // All validations passed
    return ValidationResult(true)
}
