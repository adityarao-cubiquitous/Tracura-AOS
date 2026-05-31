package com.cubiquitous.tracura.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.AuthState
import com.cubiquitous.tracura.model.CustomerSelectionItem
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.UserRole
import com.cubiquitous.tracura.repository.AuthRepository
import com.cubiquitous.tracura.repository.ProjectRepository
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.content.Context
import android.content.SharedPreferences
import com.cubiquitous.tracura.utils.DeviceUtils
import com.cubiquitous.tracura.utils.OTPManager
import com.cubiquitous.tracura.service.OTPDetectionService
import com.cubiquitous.tracura.service.FCMTokenManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val firebaseAuth: FirebaseAuth,
    private val fcmTokenManager: FCMTokenManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    companion object {
        private const val AUTH_PREFS = "auth_prefs"
        private const val SELECTED_CUSTOMER_ID_KEY = "SelectedCustomerID"
    }

    fun saveDeviceInfoAfterLogin(context: Context) {
        viewModelScope.launch {
            try {
                val deviceInfo = DeviceUtils.collectDeviceInfo(context)
                val phoneNumber = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: return@launch

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(phoneNumber)
                    .update("deviceInfo", deviceInfo)
                    .addOnSuccessListener {
                    }
                    .addOnFailureListener { e ->
                    }

                // FCM token will be saved by the FCM service automatically

            } catch (e: Exception) {
            }
        }
    }


    // Main authentication state
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // Current user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Phone authentication specific state
    private val _verificationId = MutableStateFlow<String?>(null)
    val verificationId: StateFlow<String?> = _verificationId.asStateFlow()
    
    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()
    
    private val _resendToken = MutableStateFlow<PhoneAuthProvider.ForceResendingToken?>(null)
    val resendToken: StateFlow<PhoneAuthProvider.ForceResendingToken?> = _resendToken.asStateFlow()
    
    // Track if user completed verification in this session
    private val _hasCompletedVerification = MutableStateFlow(false)
    val hasCompletedVerification: StateFlow<Boolean> = _hasCompletedVerification.asStateFlow()
    
    // Track access restriction state
    private val _isAccessRestricted = MutableStateFlow(false)
    val isAccessRestricted: StateFlow<Boolean> = _isAccessRestricted.asStateFlow()
    
    private val _restrictedPhoneNumber = MutableStateFlow<String?>(null)
    val restrictedPhoneNumber: StateFlow<String?> = _restrictedPhoneNumber.asStateFlow()
    
    // OTP Auto-fill state
    private val _detectedOTP = MutableStateFlow<String?>(null)
    val detectedOTP: StateFlow<String?> = _detectedOTP.asStateFlow()
    
    private val _isOTPAutoFilled = MutableStateFlow(false)
    val isOTPAutoFilled: StateFlow<Boolean> = _isOTPAutoFilled.asStateFlow()
    
    // Development skip session tracking
    private val _isDevelopmentSkipUser = MutableStateFlow(false)
    val isDevelopmentSkipUser: StateFlow<Boolean> = _isDevelopmentSkipUser.asStateFlow()
    
    // Business Head Phone state
    private val _businessHeadPhone = MutableStateFlow<String?>(null)
    val businessHeadPhone: StateFlow<String?> = _businessHeadPhone.asStateFlow()

    private val _customerIDMap = MutableStateFlow<Map<String, UserRole>>(emptyMap())
    val customerIDMap: StateFlow<Map<String, UserRole>> = _customerIDMap.asStateFlow()

    private val _currentCustomerId = MutableStateFlow<String?>(null)
    val currentCustomerId: StateFlow<String?> = _currentCustomerId.asStateFlow()

    private val _requiresCustomerSelection = MutableStateFlow(false)
    val requiresCustomerSelection: StateFlow<Boolean> = _requiresCustomerSelection.asStateFlow()

    private val _customerSelectionItems = MutableStateFlow<List<CustomerSelectionItem>>(emptyList())
    val customerSelectionItems: StateFlow<List<CustomerSelectionItem>> = _customerSelectionItems.asStateFlow()

    private val _isLoadingCustomerSelection = MutableStateFlow(false)
    val isLoadingCustomerSelection: StateFlow<Boolean> = _isLoadingCustomerSelection.asStateFlow()

    private val _customerSelectionError = MutableStateFlow<String?>(null)
    val customerSelectionError: StateFlow<String?> = _customerSelectionError.asStateFlow()


    // Debug configuration for development builds
    private val isDebugBuild = true // Set to false for production releases
    
    // Development mode flag to bypass security checks
    private val isDevelopmentMode = true // Set to false for production releases

    init {


        // Add a small delay to ensure proper initialization
        viewModelScope.launch {
            delay(100)
            initializeAuthState()
        }
    }
    
    /**
     * Initialize authentication state - checks for both Firebase and development skip sessions
     * NOTE: This can cause issues during testing if an old session exists
     */
    private fun initializeAuthState() {
        viewModelScope.launch {
            // Check if we're in the middle of an OTP flow
            if (_otpSent.value) {
                setUnauthenticatedState()
                return@launch
            }
            
            _authState.value = _authState.value.copy(isLoading = true)
            
            // First check for Firebase authentication
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {


                val user = authRepository.getCurrentUserFromFirebase()
                if (user != null) {
                    val resolvedUser = resolveCustomerContextForPhoneUser(user)
                    setAuthenticatedUser(resolvedUser, isDevelopmentSkip = false)
                    
                    // Save FCM token for cached user
                    if (user.role == UserRole.BUSINESS_HEAD) {
                        // For production heads, use customerId if available, otherwise fallback to uid
                        val customerId = user.customerId ?: user.uid
                        if (customerId.isNotBlank()) {
                            fcmTokenManager.saveFCMTokenToCustomer(customerId)
                        }
                    } else if (user.phone.isNotBlank()) {
                        fcmTokenManager.saveFCMTokenToUser(user.phone)
                    }
                    
                    return@launch
                } else {
                    firebaseAuth.signOut()
                }
            }
            
            // Check for development skip session
            val developmentUser = _currentUser.value
            if (developmentUser != null && developmentUser.uid.startsWith("dev_test_user_")) {
                setAuthenticatedUser(developmentUser, isDevelopmentSkip = true)
                return@launch
            }
            
            // No authentication found
            setUnauthenticatedState()
        }
    }
    
    /**
     * Set user as authenticated
     */
    private fun setAuthenticatedUser(user: User, isDevelopmentSkip: Boolean) {
        // Update both state flows to ensure consistency
        _currentUser.value = user
        _isDevelopmentSkipUser.value = isDevelopmentSkip
        _hasCompletedVerification.value = true
        
        // Update auth state with the user
        _authState.value = _authState.value.copy(
            isAuthenticated = true,
            user = user,
            isLoading = false,
            error = null
        )
        
        // Save current user ID to SharedPreferences for FCM service
        saveCurrentUserIdToPreferences(user.phone)
    }
    
    /**
     * Save current user ID to SharedPreferences for FCM service to use
     */
    private fun saveCurrentUserIdToPreferences(userId: String) {
        try {
            val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("current_user_phone", userId)
                .putString("current_user_id", userId)
                .apply()
        } catch (e: Exception) {
        }
    }
    
    /**
     * Set unauthenticated state
     */
    private fun setUnauthenticatedState() {
        _currentUser.value = null
        _isDevelopmentSkipUser.value = false
        _hasCompletedVerification.value = false
        _customerIDMap.value = emptyMap()
        _currentCustomerId.value = null
        _requiresCustomerSelection.value = false
        _customerSelectionItems.value = emptyList()
        _customerSelectionError.value = null
        _authState.value = _authState.value.copy(
            isAuthenticated = false,
            user = null,
            isLoading = false,
            error = null
        )
        clearPhoneAuthState()
    }

    private fun normalizePhone(phoneNumber: String): String {
        return phoneNumber
            .removePrefix("+91")
            .replace(" ", "")
            .trim()
    }

    private fun parseRole(roleValue: Any?): UserRole {
        val normalizedRole = (roleValue as? String)
            ?.trim()
            ?.replace("-", "_")
            ?.replace(" ", "_")
            ?.uppercase()

        return when (normalizedRole) {
            "APPROVER" -> UserRole.APPROVER
            "ADMIN" -> UserRole.ADMIN
            "BUSINESS_HEAD", "BUSINESSHEAD" -> UserRole.BUSINESS_HEAD
            "MANAGER" -> UserRole.MANAGER
            else -> UserRole.USER
        }
    }

    private fun getSelectedCustomerIdFromStorage(): String? {
        val prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(SELECTED_CUSTOMER_ID_KEY, null)?.takeIf { it.isNotBlank() }
    }

    private fun persistSelectedCustomerId(customerId: String?) {
        val prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (customerId.isNullOrBlank()) {
                remove(SELECTED_CUSTOMER_ID_KEY)
            } else {
                putString(SELECTED_CUSTOMER_ID_KEY, customerId)
            }
        }.apply()
    }

    private suspend fun resolveCustomerContextForPhoneUser(user: User): User {
        if (user.role == UserRole.BUSINESS_HEAD) {
            _customerIDMap.value = emptyMap()
            _requiresCustomerSelection.value = false
            _currentCustomerId.value = user.customerId
            return user
        }

        return try {
            val firebasePhone = firebaseAuth.currentUser?.phoneNumber
            val fallbackPhone = if (user.phone.isNotBlank()) user.phone else firebasePhone.orEmpty()
            val cleanPhone = normalizePhone(fallbackPhone)

            if (cleanPhone.isBlank()) {
                _customerIDMap.value = emptyMap()
                _requiresCustomerSelection.value = false
                _currentCustomerId.value = user.customerId
                return user
            }

            val relationDocs = FirebaseFirestore.getInstance()
                .collection("CustomerUserRelation")
                .whereEqualTo("userID", cleanPhone)
                .get()
                .await()

            val parsedCustomerMap: Map<String, UserRole> = relationDocs.documents
                .mapNotNull { doc ->
                    val customerId = doc.getString("CustomerId")?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    val role = parseRole(doc.getString("role"))
                    customerId to role
                }
                .toMap()

            _customerIDMap.value = parsedCustomerMap

            when {
                parsedCustomerMap.size == 1 -> {
                    val selectedCustomerId = parsedCustomerMap.keys.first()
                    val selectedRole = parsedCustomerMap[selectedCustomerId] ?: user.role

                    _currentCustomerId.value = selectedCustomerId
                    _requiresCustomerSelection.value = false
                    persistSelectedCustomerId(selectedCustomerId)

                    user.copy(
                        customerId = selectedCustomerId,
                        role = selectedRole,
                    )
                }

                parsedCustomerMap.size > 1 -> {
                    val persistedSelection = getSelectedCustomerIdFromStorage()

                    if (!persistedSelection.isNullOrBlank() && parsedCustomerMap.containsKey(persistedSelection)) {
                        val selectedRole = parsedCustomerMap[persistedSelection] ?: user.role
                        _currentCustomerId.value = persistedSelection
                        _requiresCustomerSelection.value = false

                        user.copy(
                            customerId = persistedSelection,
                            role = selectedRole,
                        )
                    } else {
                        _currentCustomerId.value = null
                        _requiresCustomerSelection.value = true
                        persistSelectedCustomerId(null)
                        user
                    }
                }

                else -> {
                    val fallbackCustomerId = user.customerId
                        ?: cleanPhone.takeIf { it.isNotBlank() }

                    _currentCustomerId.value = fallbackCustomerId
                    _requiresCustomerSelection.value = false
                    persistSelectedCustomerId(fallbackCustomerId)

                    user.copy(customerId = fallbackCustomerId)
                }
            }
        } catch (_: Exception) {
            _customerIDMap.value = emptyMap()
            _requiresCustomerSelection.value = false
            _currentCustomerId.value = user.customerId
            user
        }
    }

    fun loadCustomerSelections() {
        viewModelScope.launch {
            _isLoadingCustomerSelection.value = true
            _customerSelectionError.value = null

            try {
                val idRoleMap = _customerIDMap.value
                if (idRoleMap.isEmpty()) {
                    _customerSelectionItems.value = emptyList()
                    _isLoadingCustomerSelection.value = false
                    return@launch
                }

                val items = coroutineScope {
                    idRoleMap.entries.map { (customerId, role) ->
                        async {
                            val customerDoc = FirebaseFirestore.getInstance()
                                .collection("customers")
                                .document(customerId)
                                .get()
                                .await()

                            val businessName = customerDoc.getString("businessName")
                                ?.takeIf { it.isNotBlank() }
                                ?: customerDoc.getString("name")
                                ?.takeIf { it.isNotBlank() }
                                ?: customerId

                            val businessType = customerDoc.getString("businessType").orEmpty()

                            CustomerSelectionItem(
                                customerId = customerId,
                                businessName = businessName,
                                businessType = businessType,
                                role = role,
                            )
                        }
                    }.awaitAll()
                }.sortedBy { it.businessName.lowercase() }

                _customerSelectionItems.value = items
            } catch (e: Exception) {
                _customerSelectionError.value = e.message ?: "Failed to load customers"
                _customerSelectionItems.value = emptyList()
            } finally {
                _isLoadingCustomerSelection.value = false
            }
        }
    }

    fun selectCustomer(customerId: String): Boolean {
        val idRoleMap = _customerIDMap.value
        val selectedRole = idRoleMap[customerId] ?: return false
        val currentUser = _authState.value.user ?: return false

        _currentCustomerId.value = customerId
        _requiresCustomerSelection.value = false
        persistSelectedCustomerId(customerId)

        val updatedUser = currentUser.copy(
            customerId = customerId,
            role = selectedRole,
        )

        _currentUser.value = updatedUser
        _authState.value = _authState.value.copy(user = updatedUser)
        return true
    }
    
    /**
     * Clear phone authentication state
     */
    fun clearPhoneAuthState() {
        _verificationId.value = null
        _otpSent.value = false
        _resendToken.value = null
        _isAccessRestricted.value = false
        _restrictedPhoneNumber.value = null
        _detectedOTP.value = null
        _isOTPAutoFilled.value = false
    }
    
    /**
     * Initialize OTP detection monitoring
     * This method starts monitoring for OTP detection from SMS
     */
    fun initializeOTPDetection(context: Context) {
        viewModelScope.launch {
            // Start the comprehensive OTP detection service
            val otpDetectionService = OTPDetectionService(context)
            otpDetectionService.startOTPDetection()
            
            
            // Monitor OTP detection from OTPManager
            combine(
                OTPManager.detectedOTP,
                OTPManager.isOTPDetected
            ) { detectedOTP, isDetected ->
                if (isDetected && detectedOTP != null && detectedOTP.isNotEmpty()) {
                    _detectedOTP.value = detectedOTP
                    _isOTPAutoFilled.value = true
                    
                    // Auto-verify the OTP if we have a verification ID
                    if (_verificationId.value != null) {
                        verifyOTP(detectedOTP)
                    }
                }
            }.collect { }
        }
    }
    
    /**
     * Get the detected OTP for auto-filling
     */
    fun getDetectedOTP(): String? {
        return _detectedOTP.value
    }
    
    /**
     * Clear the detected OTP after successful verification
     */
    fun clearDetectedOTP() {
        _detectedOTP.value = null
        _isOTPAutoFilled.value = false
        OTPManager.clearDetectedOTP()
    }
    
    /**
     * Check if OTP was auto-filled
     */
    fun isOTPAutoFilled(): Boolean {
        return _isOTPAutoFilled.value
    }
    
    /**
     * Development bypass for Play Integrity checks
     * This method sends real OTP but bypasses Play Integrity verification
     */
    fun bypassPlayIntegrityForDevelopment(phoneNumber: String) {
        if (!isDevelopmentMode) {
            return
        }

        viewModelScope.launch {
            try {
                // Instead of simulating, actually send OTP but with development settings
                _authState.value = _authState.value.copy(isLoading = true, error = null)
                
                // Create a simple verification ID for development
                val devVerificationId = "dev_verification_${System.currentTimeMillis()}"
                _verificationId.value = devVerificationId
                
                // Simulate OTP sent successfully
                _otpSent.value = true
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Development bypass failed: ${e.message}"
                )
            }
        }
    }

    // Send OTP to phone number
    fun sendOTP(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            _otpSent.value = false
            // Try Firebase OTP first, fallback to development mode if it fails
            try {
                val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(phoneAuthCallbacks)
                    // Note: Play Integrity checks can be disabled in Firebase Console
                    // Go to Authentication > Settings > Advanced > App verification
                    .build()
                
                PhoneAuthProvider.verifyPhoneNumber(options)
                
            } catch (e: Exception) {
                // If Firebase fails due to Play Integrity, use development bypass
                if (isDevelopmentMode && (e.message?.contains("Play Integrity") == true || 
                    e.message?.contains("reCAPTCHA") == true || 
                    e.message?.contains("app identifier") == true)) {
                    bypassPlayIntegrityForDevelopment(phoneNumber)
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Failed to send OTP: ${e.message}"
                    )
                }
            }
        }
    }
    
    // Resend OTP
    fun resendOTP(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            val token = _resendToken.value
            if (token != null) {
                _authState.value = _authState.value.copy(isLoading = true, error = null)
                _otpSent.value = false
                try {
                    val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setCallbacks(phoneAuthCallbacks)
                        .setForceResendingToken(token)
                        // Note: Play Integrity checks can be disabled in Firebase Console
                        // Go to Authentication > Settings > Advanced > App verification
                        .build()
                    
                    PhoneAuthProvider.verifyPhoneNumber(options)
                    
                } catch (e: Exception) {
                    // If Firebase fails due to Play Integrity, use development bypass
                    if (isDevelopmentMode && (e.message?.contains("Play Integrity") == true || 
                        e.message?.contains("reCAPTCHA") == true || 
                        e.message?.contains("app identifier") == true)) {
                        bypassPlayIntegrityForDevelopment(phoneNumber)
                    } else {
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            error = "Failed to resend OTP: ${e.message}"
                        )
                    }
                }
            } else {
                _authState.value = _authState.value.copy(
                    error = "Cannot resend OTP at this time"
                )
            }
        }
    }
    
    // Phone Authentication Callbacks
    private val phoneAuthCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Auto-verification completed, sign in with the credential
            signInWithCredential(credential)
        }
        
        override fun onVerificationFailed(e: FirebaseException) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Phone verification failed: ${e.message}"
            )
            _otpSent.value = false
        }
        
        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            _verificationId.value = verificationId
            _resendToken.value = token
            _otpSent.value = true
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = null
            )
        }
        
        override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
            // This is called when the auto-retrieval timeout expires
        }
    }
    
    // Verify OTP and sign in
    fun verifyOTP(otpCode: String) {
        viewModelScope.launch {
            val currentVerificationId = _verificationId.value


            if (currentVerificationId != null) {
                _authState.value = _authState.value.copy(isLoading = true, error = null)
                try {
                    val credential = PhoneAuthProvider.getCredential(currentVerificationId, otpCode)
                    signInWithCredential(credential)
                } catch (e: Exception) {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Invalid OTP. Please try again."
                    )
                }
            } else {
                _authState.value = _authState.value.copy(
                    error = "No verification in progress. Please request OTP again."
                )
            }
        }
    }

    fun signInWithCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            authRepository.signInWithPhoneCredential(credential)
                .onSuccess { user ->
                    val resolvedUser = resolveCustomerContextForPhoneUser(user)
                    setAuthenticatedUser(resolvedUser, isDevelopmentSkip = false)
                    
                    // Save FCM token based on user role (run in background to not block UI)
                    viewModelScope.launch {
                        try {
                            // For production head, save FCM token to customers collection FCMlist
                            if (user.role == UserRole.BUSINESS_HEAD) {
                                // Use customerId if available, otherwise fallback to uid
                                val customerId = user.customerId ?: user.uid
                                if (customerId.isNotBlank()) {
                                    fcmTokenManager.saveFCMTokenToCustomer(customerId)
                                }
                            }
                            
                            // For all users (including production heads), also save to users collection if phone exists
                            if (user.phone.isNotBlank()) {
                                fcmTokenManager.saveFCMTokenToUser(user.phone)
                            }
                        } catch (e: Exception) {
                        }
                    }
                    
                    // Clear detected OTP after successful verification
                    clearDetectedOTP()
                    
                    // Ensure state is fully synchronized
                    delay(200)
                }
                .onFailure { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Authentication failed: ${error.message}"
                    )
                }
        }
    }
    
    fun selectProject(projectId: String) {
        viewModelScope.launch {
            val project = projectRepository.getProjectById(projectId)
            _authState.value = _authState.value.copy(
                selectedProjectId = projectId,
                selectedProject = project
            )
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            performLogout()
        }
    }
    
    /**
     * Suspend function to perform logout - can be awaited
     * Ensures FCM token deletion completes before clearing state
     */
    suspend fun performLogout() {
        // Get current user info before clearing state (needed for FCM token cleanup)
        val currentUser = _authState.value.user
        val userPhone = currentUser?.phone ?: ""
        val userUid = currentUser?.uid ?: ""
        val userRole = currentUser?.role
        // Remove FCM token FIRST - ensure it completes before clearing state
        // This is important to prevent receiving notifications after logout
        try {
            // For production head, remove from customers FCMlist first
            if (userRole == UserRole.BUSINESS_HEAD) {
                // Use customerId if available, otherwise fallback to uid
                val customerId = currentUser?.customerId ?: userUid
                if (customerId.isNotBlank()) {
                    fcmTokenManager.removeFCMTokenFromCustomer(customerId)
                }
            }
            
            // For all users (including production heads), also remove from users collection if phone exists
            if (userPhone.isNotBlank()) {
                fcmTokenManager.deleteFCMTokenFromUser(userPhone)
            } else {
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Continue with logout even if FCM token removal fails
        }
        
        // NOW: Sign out from Firebase Auth (this is what caches the session!)
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
        }
        
        // Clear the repository
        authRepository.signOut()
        persistSelectedCustomerId(null)
        
        // Clear all state - this makes the UI update immediately
        setUnauthenticatedState()
    }
    
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
    
    fun setError(error: String) {
        _authState.value = _authState.value.copy(error = error)
    }
    
    // Email/Password Authentication for Production Head
    fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        phoneNumber: String?,
        businessName: String,
        businessType: String?,
        location: String?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            authRepository.signUpWithEmail(
                name = name,
                email = email,
                password = password,
                phoneNumber = phoneNumber,
                businessName = businessName,
                businessType = businessType,
                location = location
            )
                .onSuccess { user ->
                    setAuthenticatedUser(user, isDevelopmentSkip = false)

                    viewModelScope.launch {
                        try {
                            val customerId = user.customerId ?: user.uid
                            if (customerId.isNotBlank()) {
                                fcmTokenManager.saveFCMTokenToCustomer(customerId)
                            }

                            if (user.phone.isNotBlank()) {
                                fcmTokenManager.saveFCMTokenToUser(user.phone)
                            }
                        } catch (_: Exception) {
                        }
                    }

                    delay(200)
                    onResult(true)
                }
                .onFailure { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Sign up failed: ${error.message}"
                    )
                    onResult(false)
                }
        }
    }
    
    fun signInWithEmail(
        email: String,
        password: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            authRepository.signInWithEmail(email, password)
                .onSuccess { user ->
                    setAuthenticatedUser(user, isDevelopmentSkip = false)
                    
                    // Save FCM token based on user role (run in background to not block UI)
                    viewModelScope.launch {
                        try {
                            // For production head, save FCM token to customers collection FCMlist
                            if (user.role == UserRole.BUSINESS_HEAD) {
                                // Use customerId if available, otherwise fallback to uid
                                val customerId = user.customerId ?: user.uid
                                if (customerId.isNotBlank()) {
                                    fcmTokenManager.saveFCMTokenToCustomer(customerId)
                                }
                            }
                            
                            // For all users (including production heads), also save to users collection if phone exists
                            if (user.phone.isNotBlank()) {
                                fcmTokenManager.saveFCMTokenToUser(user.phone)
                            }
                        } catch (e: Exception) {
                        }
                    }
                    
                    // Ensure state is fully synchronized
                    delay(200)
                    onResult(true)
                }
                .onFailure { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Sign in failed: ${error.message}"
                    )
                    onResult(false)
                }
        }
    }
    
    // Check if current authentication is from development skip
    fun isDevelopmentSkipUser(): Boolean {
        return _isDevelopmentSkipUser.value
    }
    
    // Force check and restore authentication state
    fun forceCheckAuthState() {
        viewModelScope.launch {


            // Add a small delay to ensure proper state synchronization
            delay(100)
            initializeAuthState()
            
            // Log the final state after initialization
            delay(100)
            // If we have a current user but auth state is not synchronized, fix it
            if (_currentUser.value != null && !_authState.value.isAuthenticated) {
                setAuthenticatedUser(_currentUser.value!!, _isDevelopmentSkipUser.value)
            }
        }
    }
    
    // Force refresh user data from Firebase
    fun refreshUserData() {
        viewModelScope.launch {
            // If this is a development skip user, don't refresh from Firebase
            if (_isDevelopmentSkipUser.value) {
                return@launch
            }
            
            val user = authRepository.getCurrentUserFromFirebase()
            if (user != null) {
                val resolvedUser = resolveCustomerContextForPhoneUser(user)
                setAuthenticatedUser(resolvedUser, isDevelopmentSkip = false)
            } else {
                logout()
            }
        }
    }

    
    // Clear access restriction state
    fun clearAccessRestriction() {
        _isAccessRestricted.value = false
        _restrictedPhoneNumber.value = null
    }
    
    fun loadBusinessHeadPhone(userPhone: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("AuthViewModel", "🔍 Loading business head phone for user: $userPhone")
                val bhPhone = authRepository.getBusinessHeadPhone(userPhone)
                _businessHeadPhone.value = bhPhone
                android.util.Log.d("AuthViewModel", "✅ Business head phone loaded: $bhPhone")
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "❌ Error loading business head phone", e)
            }
        }
    }
    
    // Get user by phone number
    suspend fun getUserByPhoneNumber(phoneNumber: String): User? {
        return authRepository.getUserByPhoneNumber(phoneNumber)
    }
    
    // Get multiple users by phone numbers (batch query)
    suspend fun getUsersByPhoneNumbers(phoneNumbers: List<String>): List<User> {
        return authRepository.getUsersByPhoneNumbers(phoneNumbers)
    }
    
    // Get userRole using phone number logic
    suspend fun getUserRoleByPhoneLogic(): String? {
        return authRepository.getUserRoleByPhoneLogic()
    }

    // Development skip - for testing purposes only (phone number based)
    fun skipOTPForDevelopment(phoneNumber: String, onNavigationCallback: (UserRole) -> Unit) {
        viewModelScope.launch {

            // IMPORTANT: Clear any previous authentication state first
            // Sign out from Firebase to clear any cached authentication
            try {
                authRepository.signOut()
            } catch (e: Exception) {
            }
            
            _authState.value = _authState.value.copy(isAuthenticated = false, user = null, isLoading = false, error = null)
            _currentUser.value = null
            _isDevelopmentSkipUser.value = false
            _hasCompletedVerification.value = false
            
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            try {
                // Small delay to show loading animation
                delay(300)
                
                // Try to get actual user from Firebase first
                val actualUser = authRepository.getUserByPhoneNumber(phoneNumber)
                
                val testUser = if (actualUser != null) {





                    // Use the actual user data but with a development UID
                    val user = actualUser.copy(
                        uid = "dev_test_user_${phoneNumber}",
                        phone = phoneNumber
                    )
                    user
                } else {

                    // Create default test user if no actual user found
                    User(
                        uid = "dev_test_user_${phoneNumber}",
                        name = "Test User ($phoneNumber)",
                        email = "test@example.com",
                        phone = phoneNumber,
                        role = UserRole.USER, // Default to USER role
                        createdAt = System.currentTimeMillis(),
                        isActive = true,
                        assignedProjects = listOf("project1", "project2")
                    )
                }
                
                // Set the user as authenticated
                setAuthenticatedUser(testUser, isDevelopmentSkip = true)
                
                // Save FCM token (run in background to not block UI)
                viewModelScope.launch {
                    try {
                        // For production head, save FCM token to customers collection FCMlist
                        if (testUser.role == UserRole.BUSINESS_HEAD) {
                            // Use customerId if available, otherwise fallback to uid
                            val customerId = testUser.customerId ?: testUser.uid
                            if (customerId.isNotBlank()) {
                                fcmTokenManager.saveFCMTokenToCustomer(customerId)
                            }
                        }
                        
                        // For all users (including production heads), also save to users collection if phone exists
                        if (testUser.phone.isNotBlank()) {
                            fcmTokenManager.saveFCMTokenToUser(testUser.phone)
                        }
                    } catch (e: Exception) {
                    }
                }
                
                // Ensure state is fully synchronized before navigation
                delay(200)
                
                // Double-check that the state is properly set
                if (_authState.value.isAuthenticated && _authState.value.user != null) {
                    // Trigger direct navigation
                    onNavigationCallback(testUser.role)
                } else {
                    setAuthenticatedUser(testUser, isDevelopmentSkip = true)
                    delay(100)
                    onNavigationCallback(testUser.role)
                }
                
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Development skip failed: ${e.message}"
                )
            }
        }
    }
}
