package com.cubiquitous.tracura.ui.view.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.utils.PermissionUtils
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.cubiquitous.tracura.ui.view.auth.LoginType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    onVerificationSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val palette = authUiPalette()
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            palette.accent,
            palette.accentSecondary,
            palette.accentTertiary
        )
    )
    var otpCode by remember { mutableStateOf("") }
    var selectedLoginType by remember { mutableStateOf(LoginType.PHONE_LOGIN) }
    val authState by authViewModel.authState.collectAsState()
    val verificationId by authViewModel.verificationId.collectAsState()
    val otpSent by authViewModel.otpSent.collectAsState()
    val detectedOTP by authViewModel.detectedOTP.collectAsState()
    val isOTPAutoFilled by authViewModel.isOTPAutoFilled.collectAsState()
    val context = LocalContext.current
    var isVerifying by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var resendCountdown by remember { mutableStateOf(60) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            android.util.Log.d("OTPScreen", "✅ SMS permissions granted")
        } else {
            android.util.Log.d("OTPScreen", "❌ SMS permissions denied")
        }
    }

    LaunchedEffect(Unit) {
        android.util.Log.d("OTPScreen", "🔍 AuthViewModel instance: ${authViewModel.hashCode()}")
        android.util.Log.d("OTPScreen", "🔍 Initial verification ID: $verificationId")
        android.util.Log.d("OTPScreen", "🔍 Initial OTP sent status: $otpSent")

        if (!PermissionUtils.hasSMSPermissions(context)) {
            android.util.Log.d("OTPScreen", "🔐 Requesting SMS permissions for OTP auto-fill")
            smsPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                )
            )
        }

        authViewModel.initializeOTPDetection(context)

        if (verificationId == null && !otpSent) {
            android.util.Log.d("OTPScreen", "🔄 No verification ID found, triggering OTP resend")
            authViewModel.sendOTP(phoneNumber, context as Activity)
        }
    }

    LaunchedEffect(detectedOTP, isOTPAutoFilled) {
        if (detectedOTP != null && isOTPAutoFilled && otpCode.isEmpty()) {
            android.util.Log.d("OTPScreen", "🔑 Auto-filling OTP: $detectedOTP")
            otpCode = detectedOTP!!
            showError = false
            authViewModel.clearError()
        }
    }

    LaunchedEffect(authState.isAuthenticated, authState.user) {
        if (authState.isAuthenticated && authState.user != null) {
            android.util.Log.d("OTPScreen", "✅ Authentication complete, user loaded: ${authState.user?.name} with role: ${authState.user?.role}")
            isVerifying = false
            onVerificationSuccess()
        }
    }

    LaunchedEffect(authState.error) {
        if (authState.error != null) {
            isVerifying = false
            showError = true
            errorMessage = authState.error ?: "Authentication failed"
        }
    }

    LaunchedEffect(otpSent) {
        if (otpSent) {
            resendCountdown = 60
            while (resendCountdown > 0) {
                kotlinx.coroutines.delay(1000)
                resendCountdown--
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            authViewModel.clearPhoneAuthState()
        }
    }

    // ─── Root: gradient background ────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.tier1Background)
    ) {
        // Decorative circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.accent.copy(alpha = 0.22f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // ─── Top nav ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        authViewModel.clearPhoneAuthState()
                        onNavigateBack()
                    }
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.primaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Shield icon ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(palette.tier3Field),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verify Your Phone",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Code sent to  $phoneNumber",
                fontSize = 14.sp,
                color = palette.secondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ─── OTP Card ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = palette.tier2Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 36.dp)
                ) {
                    Text(
                        text = "Enter Verification Code",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter the 6-digit code we sent you",
                        fontSize = 14.sp,
                        color = palette.secondaryText
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Counter row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Verification Code",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primaryText
                        )
                        Text(
                            text = "${otpCode.length}/6",
                            fontSize = 12.sp,
                            color = palette.accentSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = {
                            if (it.length <= 6) {
                                otpCode = it
                                showError = false
                                authViewModel.clearError()
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        placeholder = {
                            Text("Enter 6-digit code", color = palette.secondaryText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = showError || authState.error != null,
                        enabled = !isVerifying && !authState.isLoading,
                        colors = brandTextFieldColors()
                    )

                    if (showError || authState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage.ifEmpty { authState.error ?: "An error occurred" },
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    GradientButton(
                        text = "Verify",
                        palette = palette,
                        gradientBrush = gradientBrush,
                        onClick = {
                            if (otpCode.length == 6) {
                                isVerifying = true
                                showError = false
                                android.util.Log.d("OTPScreen", "🔄 Verifying OTP: $otpCode")
                                authViewModel.verifyOTP(otpCode)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = otpCode.length == 6 && !isVerifying && !authState.isLoading,
                        isLoading = isVerifying || authState.isLoading,
                        leadingIcon = {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resend / Change Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (resendCountdown > 0) {
                            Text(
                                text = "Resend code in ",
                                fontSize = 14.sp,
                                color = palette.secondaryText
                            )
                            Text(
                                text = "${resendCountdown}s",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentSecondary
                            )
                        } else {
                            TextButton(
                                onClick = {
                                    authViewModel.sendOTP(phoneNumber, context as Activity)
                                }
                            ) {
                                Text(
                                    text = "Resend Code",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.accent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            authViewModel.clearPhoneAuthState()
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Change Phone Number",
                            fontSize = 14.sp,
                            color = palette.secondaryText
                        )
                    }
                }
            }
        }
    }
}
