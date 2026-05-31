package com.cubiquitous.tracura.ui.view.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSignUpScreen(
    onNavigateBack: () -> Unit,
    onSignUpSuccess: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val palette = authUiPalette()
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(palette.accent, palette.accentSecondary, palette.accentTertiary)
    )
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var businessTypeExpanded by remember { mutableStateOf(false) }

    val businessTypeOptions = listOf("Construction", "Interior Design", "Media")

    val authState by authViewModel.authState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState.error) {
        authState.error?.let { errorMessage = it }
    }

    val passwordMismatch = remember(password, confirmPassword) {
        password.isNotBlank() && confirmPassword.isNotBlank() && password != confirmPassword
    }

    val emailInvalid = remember(email) {
        email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    val isCreateEnabled = remember(name, email, password, confirmPassword, businessName, businessType, phoneNumber) {
        name.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                confirmPassword.isNotBlank() &&
                password == confirmPassword &&
                businessName.isNotBlank() &&
                businessType.isNotBlank() &&
                phoneNumber.isNotBlank()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.tier2Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.tier2Surface)
        ) {
            // ─── Gradient header strip ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = gradientBrush
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text(
                            text = "Cancel",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Text(
                        text = "Sign Up",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(72.dp))
                }
            }

            // ─── Scrollable content ───────────────────────────────────────
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Create Account",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                    Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Sign up to get started",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = palette.secondaryText,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                    Spacer(modifier = Modifier.height(28.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        // ── Name ──────────────────────────────────────────────
                        SignUpField(label = "Full Name", palette = palette) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it.filter { ch -> ch.isLetter() || ch.isWhitespace() }
                                },
                                placeholder = { Text("Enter your full name", color = palette.secondaryText) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = "Name", tint = palette.accentSecondary)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = brandTextFieldColors()
                            )
                        }

                        // ── Email ─────────────────────────────────────────────
                        SignUpField(label = "Email Address", palette = palette) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it.lowercase() },
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
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(12.dp),
                                isError = emailInvalid,
                                supportingText = if (emailInvalid) {
                                    { Text("Invalid email address", color = palette.danger, fontSize = 12.sp) }
                                } else null,
                                colors = brandTextFieldColors()
                            )
                            if (!emailInvalid) Spacer(modifier = Modifier.height((-16).dp))
                        }

                        // ── Password ──────────────────────────────────────────
                        SignUpField(label = "Password", palette = palette) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("Min 6 characters", color = palette.secondaryText) },
                                leadingIcon = {
                                    Icon(
                                        painterResource(id = R.drawable.lock),
                                        contentDescription = "Password",
                                        tint = palette.accentSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = brandTextFieldColors()
                            )
                        }

                        // ── Confirm Password ──────────────────────────────────
                        SignUpField(label = "Confirm Password", palette = palette) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = { Text("Re-enter password", color = palette.secondaryText) },
                                leadingIcon = {
                                    Icon(
                                        painterResource(id = R.drawable.lock1),
                                        contentDescription = "Confirm Password",
                                        tint = palette.accentSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (confirmPasswordVisible) "Hide" else "Show",
                                            tint = palette.accentSecondary
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                isError = passwordMismatch,
                                supportingText = if (passwordMismatch) {
                                    { Text("Passwords do not match", color = palette.danger, fontSize = 12.sp) }
                                } else null,
                                colors = brandTextFieldColors()
                            )
                        }

                        // ── Business Name ─────────────────────────────────────
                        SignUpField(label = "Business Name", palette = palette) {
                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { businessName = it },
                                placeholder = { Text("Enter your business name", color = palette.secondaryText) },
                                leadingIcon = {
                                    Icon(Icons.Default.Business, contentDescription = "Business", tint = palette.accentSecondary)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = brandTextFieldColors()
                            )
                        }

                        // ── Business Type ─────────────────────────────────────
                        SignUpField(label = "Business Type", palette = palette) {
                            ExposedDropdownMenuBox(
                                expanded = businessTypeExpanded,
                                onExpandedChange = { businessTypeExpanded = !businessTypeExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = businessType.ifEmpty { "" },
                                    onValueChange = { },
                                    readOnly = true,
                                    placeholder = { Text("Select business type", color = palette.secondaryText) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Work, contentDescription = "Business Type", tint = palette.accentSecondary)
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = palette.accentSecondary
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = palette.tier3Field,
                                        unfocusedContainerColor = palette.tier3Field,
                                        focusedBorderColor = palette.accentSecondary,
                                        unfocusedBorderColor = palette.outline,
                                        cursorColor = palette.accentSecondary,
                                        focusedTextColor = palette.primaryText,
                                        unfocusedTextColor = palette.primaryText,
                                        focusedLeadingIconColor = palette.accentSecondary,
                                        unfocusedLeadingIconColor = palette.accentSecondary,
                                        focusedTrailingIconColor = palette.accentSecondary,
                                        unfocusedTrailingIconColor = palette.accentSecondary,
                                        focusedPlaceholderColor = palette.secondaryText,
                                        unfocusedPlaceholderColor = palette.secondaryText
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = businessTypeExpanded,
                                    onDismissRequest = { businessTypeExpanded = false },
                                    modifier = Modifier.background(palette.tier2Surface)
                                ) {
                                    businessTypeOptions.forEachIndexed { index, type ->
                                        DropdownMenuItem(
                                            text = { Text(type, fontSize = 16.sp, color = palette.primaryText) },
                                            onClick = {
                                                businessType = type
                                                businessTypeExpanded = false
                                            },
                                            modifier = Modifier.height(52.dp)
                                        )
                                        if (index < businessTypeOptions.size - 1) {
                                            HorizontalDivider(color = palette.outline, thickness = 1.dp)
                                        }
                                    }
                                }
                            }
                        }

                        // ── Phone Number ──────────────────────────────────────
                        SignUpField(label = "Phone Number", palette = palette) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { if (it.length <= 10) phoneNumber = it },
                                placeholder = { Text("10-digit number", color = palette.secondaryText) },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = "Phone", tint = palette.accentSecondary)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                colors = brandTextFieldColors()
                            )
                        }

                        // ── Location (optional) ───────────────────────────────
                        SignUpField(label = "Location (Optional)", palette = palette) {
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                placeholder = { Text("Enter location", color = palette.secondaryText) },
                                leadingIcon = {
                                    Icon(
                                        painterResource(id = R.drawable.location),
                                        contentDescription = "Location",
                                        tint = palette.accentSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = brandTextFieldColors()
                            )
                        }

                        // ── Error ─────────────────────────────────────────────
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Create Account button ─────────────────────────────
                        GradientButton(
                            text = "Create Account",
                            palette = palette,
                            gradientBrush = gradientBrush,
                            onClick = {
                                when {
                                    name.isBlank() -> errorMessage = "Please enter your name"
                                    email.isBlank() -> errorMessage = "Please enter your email"
                                    password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                                    password != confirmPassword -> errorMessage = "Passwords do not match"
                                    businessName.isBlank() -> errorMessage = "Please enter your business name"
                                    phoneNumber.isBlank() -> errorMessage = "Please enter your phone number"
                                    else -> {
                                        errorMessage = null
                                        authViewModel.signUpWithEmail(
                                            name = name,
                                            email = email,
                                            password = password,
                                            phoneNumber = phoneNumber.takeIf { it.isNotEmpty() },
                                            businessName = businessName,
                                            businessType = businessType.takeIf { it.isNotEmpty() },
                                            location = location.takeIf { it.isNotEmpty() }
                                        ) { success ->
                                            if (success) onSignUpSuccess()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isCreateEnabled && !authState.isLoading,
                            isLoading = authState.isLoading,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Login link ────────────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Already have an account? ",
                                fontSize = 14.sp,
                                color = palette.secondaryText
                            )
                            TextButton(onClick = onNavigateBack) {
                                Text(
                                    text = "Login",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accent
                                )
                            }
                        }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ─── Label + field wrapper ────────────────────────────────────────────────────
@Composable
private fun SignUpField(
    label: String,
    palette: AuthUiPalette,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.primaryText,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        content()
    }
}
