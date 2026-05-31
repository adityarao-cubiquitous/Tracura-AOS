package com.cubiquitous.tracura.ui.view.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.AuthState
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.model.UserRole
import kotlinx.coroutines.delay
import com.cubiquitous.tracura.R

enum class LoginType {
    ADMIN_LOGIN,
    PHONE_LOGIN
}

val authGradientBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF3B5BDB),
        Color(0xFF7950F2),
        Color(0xFF12B886)
    )
)

@Composable
private fun authGradientBrushForPalette(palette: AuthUiPalette): Brush = Brush.linearGradient(
    colors = listOf(
        palette.accent,
        palette.accentSecondary,
        palette.accentTertiary
    )
)

@Composable
fun LoginScreen(
    onNavigateToOtp: (String) -> Unit,
    onSkipForDevelopment: () -> Unit,
    onNavigateToRole: (com.cubiquitous.tracura.model.UserRole) -> Unit = {},
    onNavigateToSignUp: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val palette = authUiPalette()
    val gradientBrush = authGradientBrushForPalette(palette)
    var selectedLoginType by remember { mutableStateOf(LoginType.PHONE_LOGIN) }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val otpSent by authViewModel.otpSent.collectAsState()
    val isAccessRestricted by authViewModel.isAccessRestricted.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val IosStyleFamily = FontFamily(
        Font(R.font.inter_extralight, FontWeight.Light),
        Font(R.font.inter_bold, FontWeight.Bold)
    )

    LaunchedEffect(otpSent) {
        if (otpSent) {
            android.util.Log.d("LoginScreen", "🔍 OTP sent, navigating to verification screen")
            android.util.Log.d("LoginScreen", "🔍 AuthViewModel instance: ${authViewModel.hashCode()}")
            android.util.Log.d("LoginScreen", "🔍 AuthViewModel instance: ${currentUser}")
            onNavigateToOtp("+91$phoneNumber")
        }
    }

    LaunchedEffect(isAccessRestricted) {
        if (isAccessRestricted) {
            android.util.Log.d("LoginScreen", "🚫 Access restricted, navigating to access restricted screen")
            authViewModel.clearAccessRestriction()
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            authViewModel.saveDeviceInfoAfterLogin(context)
        }
    }

    // ─── Root: full-screen gradient ──────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.tier1Background)
    ) {

        // Subtle top decorative circle
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.accent.copy(alpha = 0.28f), Color.Transparent)
                    ),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LaunchedEffect(authState.isAuthenticated, authState.user) {
                if (authState.isAuthenticated && authState.user != null) {
                    val user = authState.user!!
                    android.util.Log.d("LoginScreen", "🎯 Authenticated user detected - User: ${user.name}, Phone: ${user.phone}, Role: ${user.role}")
                    delay(200)
                    val role = user.role
                    android.util.Log.d("LoginScreen", "🎯 Processing role navigation for role: $role")
                    onNavigateToRole(role)
                }
            }

            Spacer(modifier = Modifier.height(72.dp))

            // ─── Header ──────────────────────────────────────────────────────
            Text(
                text = "Welcome Back",
                fontFamily = IosStyleFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                color = palette.primaryText,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sign in to continue",
                fontSize = 15.sp,
                color = palette.secondaryText,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ─── Tab Selector ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.tier3Field)
                    .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Admin tab
                LoginTypeTab(
                    label = "Admin Login",
                    icon = { Icon(painterResource(id = R.drawable.person), contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = selectedLoginType == LoginType.ADMIN_LOGIN,
                    palette = palette,
                    gradientBrush = gradientBrush,
                    modifier = Modifier.weight(1f)
                ) { selectedLoginType = LoginType.ADMIN_LOGIN }

                // Phone tab
                LoginTypeTab(
                    label = "Phone Login",
                    icon = { Icon(painterResource(id = R.drawable.phone), contentDescription = null, modifier = Modifier.size(20.dp)) },
                    selected = selectedLoginType == LoginType.PHONE_LOGIN,
                    palette = palette,
                    gradientBrush = gradientBrush,
                    modifier = Modifier.weight(1f)
                ) { selectedLoginType = LoginType.PHONE_LOGIN }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Login Card ───────────────────────────────────────────────────
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
                        .padding(horizontal = 28.dp, vertical = 32.dp)
                ) {
                    if (selectedLoginType == LoginType.ADMIN_LOGIN) {
                        AdminLoginContent(
                            email = email,
                            password = password,
                            passwordVisible = passwordVisible,
                            authState = authState,
                            palette = palette,
                            gradientBrush = gradientBrush,
                            onEmailChange = { email = it.lowercase(); authViewModel.clearError() },
                            onPasswordChange = { password = it; authViewModel.clearError() },
                            onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                            onSignIn = {
                                if (email.isBlank() || password.isBlank()) {
                                    authViewModel.setError("Please enter email and password")
                                } else {
                                    authViewModel.signInWithEmail(email, password) {}
                                }
                            },
                            onNavigateToSignUp = onNavigateToSignUp
                        )
                    } else {
                        PhoneLoginContent(
                            phoneNumber = phoneNumber,
                            authState = authState,
                            palette = palette,
                            gradientBrush = gradientBrush,
                            onPhoneChange = {
                                if (it.length <= 10) {
                                    phoneNumber = it
                                    authViewModel.clearError()
                                }
                            },
                            onSendCode = {
                                if (phoneNumber.length == 10) {
                                    val fullPhoneNumber = "+91$phoneNumber"
                                    authViewModel.sendOTP(fullPhoneNumber, context as android.app.Activity)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Tab Component ────────────────────────────────────────────────────────────
@Composable
private fun LoginTypeTab(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    palette: AuthUiPalette,
    gradientBrush: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) Modifier.background(brush = gradientBrush)
                else Modifier.background(Color.Transparent)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (selected) Color.White else palette.secondaryText
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color.White else palette.secondaryText
            )
        }
    }
}

// ─── Gradient Button ──────────────────────────────────────────────────────────
@Composable
fun GradientButton(
    text: String,
    palette: AuthUiPalette,
    gradientBrush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (enabled && !isLoading)
                    Modifier.background(brush = gradientBrush)
                else
                    Modifier.background(palette.disabled)
            )
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                leadingIcon?.invoke()
                if (leadingIcon != null) Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

// ─── Brand OutlinedTextField colors ──────────────────────────────────────────
@Composable
fun brandTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = authUiPalette().tier3Field,
    unfocusedContainerColor = authUiPalette().tier3Field,
    focusedBorderColor = authUiPalette().accent,
    unfocusedBorderColor = authUiPalette().outline,
    focusedLabelColor = authUiPalette().accent,
    cursorColor = authUiPalette().accent,
    focusedTextColor = authUiPalette().primaryText,
    unfocusedTextColor = authUiPalette().primaryText,
    focusedLeadingIconColor = authUiPalette().accentSecondary,
    unfocusedLeadingIconColor = authUiPalette().accentSecondary,
    focusedTrailingIconColor = authUiPalette().accentSecondary,
    unfocusedTrailingIconColor = authUiPalette().accentSecondary,
    focusedPlaceholderColor = authUiPalette().secondaryText,
    unfocusedPlaceholderColor = authUiPalette().secondaryText
)

// ─── Admin Login Content ──────────────────────────────────────────────────────
@Composable
private fun AdminLoginContent(
    email: String,
    password: String,
    passwordVisible: Boolean,
    authState: AuthState,
    palette: AuthUiPalette,
    gradientBrush: Brush,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    Text(
        text = "Administrator Access",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = palette.primaryText,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Enter your admin credentials to continue",
        fontSize = 14.sp,
        color = palette.secondaryText,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(28.dp))

    // Email
    Text(
        text = "Email Address",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = palette.primaryText,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        placeholder = { Text("your@email.com", color = palette.secondaryText) },
        leadingIcon = {
            Icon(
                painterResource(id = R.drawable.email),
                contentDescription = "Email",
                tint = palette.accentSecondary,
                modifier = Modifier.size(22.dp)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        enabled = !authState.isLoading,
        isError = authState.error != null,
        colors = brandTextFieldColors()
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Password
    Text(
        text = "Password",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = palette.primaryText,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        placeholder = { Text("Enter your password", color = palette.secondaryText) },
        leadingIcon = {
            Icon(
                painterResource(id = R.drawable.lock),
                contentDescription = "Password",
                tint = palette.accentSecondary,
                modifier = Modifier.size(22.dp)
            )
        },
        trailingIcon = {
            IconButton(onClick = onPasswordVisibilityToggle) {
                Icon(
                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = palette.accentSecondary
                )
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        enabled = !authState.isLoading,
        isError = authState.error != null,
        colors = brandTextFieldColors()
    )

    if (authState.error != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = authState.error!!,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp
        )
    }

    Spacer(modifier = Modifier.height(28.dp))

    GradientButton(
        text = "Sign In",
        palette = palette,
        gradientBrush = gradientBrush,
        onClick = onSignIn,
        modifier = Modifier.fillMaxWidth(),
        enabled = email.isNotBlank() && password.isNotBlank() && !authState.isLoading,
        isLoading = authState.isLoading,
        leadingIcon = {
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    )

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Don't have an account?", fontSize = 14.sp, color = palette.secondaryText)
        TextButton(onClick = onNavigateToSignUp) {
            Text(
                text = "Create Account",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accent
            )
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val palette = authUiPalette()
    val gradientBrush = authGradientBrushForPalette(palette)
    GradientButton(
        text = text,
        palette = palette,
        gradientBrush = gradientBrush,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
        leadingIcon = leadingIcon
    )
}

// ─── Phone Login Content ──────────────────────────────────────────────────────
@Composable
private fun PhoneLoginContent(
    phoneNumber: String,
    authState: AuthState,
    palette: AuthUiPalette,
    gradientBrush: Brush,
    onPhoneChange: (String) -> Unit,
    onSendCode: () -> Unit
) {
    Text(
        text = "Phone Verification",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = palette.primaryText,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "We'll send a verification code to your phone",
        fontSize = 14.sp,
        color = palette.secondaryText,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(28.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "Phone Number",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.primaryText
        )
        Text(
            text = "${phoneNumber.length}/10",
            fontSize = 12.sp,
            color = palette.accentSecondary
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Country code chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(palette.tier3Field)
                .border(1.dp, palette.accentSecondary, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "🇮🇳", fontSize = 16.sp)
                Text(
                    text = "+91",
                    fontSize = 15.sp,
                    color = palette.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneChange,
            placeholder = { Text("10-digit number", color = palette.secondaryText) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            enabled = !authState.isLoading,
            isError = authState.error != null,
            colors = brandTextFieldColors()
        )
    }

    if (authState.error != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = authState.error!!,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp
        )
    }

    Spacer(modifier = Modifier.height(28.dp))

    GradientButton(
        text = "Send Code",
        palette = palette,
        gradientBrush = gradientBrush,
        onClick = onSendCode,
        modifier = Modifier.fillMaxWidth(),
        enabled = phoneNumber.length == 10 && !authState.isLoading,
        isLoading = authState.isLoading,
        leadingIcon = {
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    )
}
