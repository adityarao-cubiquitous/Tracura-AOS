package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.UserRole
import kotlinx.coroutines.delay
import com.cubiquitous.tracura.R

/**
 * Pure display composable — no Firestore, no ViewModel, no side-effects.
 * All data is fetched by [AppNavHost] and passed here as plain parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onNavigateBack: () -> Unit,
    name: String,
    phone: String,
    email: String?,
    role: UserRole,
    department: String?,
    businessName: String?,
    businessType: String?,
    location: String?,
    isActive: Boolean?,
    joinDate: String?,
    isLoading: Boolean
) {
    val isDarkMode = isSystemInDarkTheme()

    val tier1Background = if (isDarkMode) Color(0xFF000000) else Color(0xFFF2F2F7)
    val tier2Surface = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val primaryTextColor = if (isDarkMode) Color.White else Color.Black
    val dividerColor = if (isDarkMode) Color(0xFF38383A) else Color(0xFFF2F2F7)

    val iosFont = FontFamily(
        Font(R.font.inter_extralight, FontWeight.Light),
        Font(R.font.inter_bold, FontWeight.Bold)
    )

    val (roleLabel, roleColor) = when (role) {
        UserRole.BUSINESS_HEAD -> "Business Head" to Color(0xFF1565C0)
        UserRole.MANAGER -> "Manager" to Color(0xFF1565C0)
        UserRole.APPROVER -> "Approver" to Color(0xFF2E7D32)
        UserRole.ADMIN -> "Admin" to Color(0xFF6A1B9A)
        UserRole.USER -> "User" to Color(0xFF01579B)
    }
    val roleBg = if (isDarkMode) roleColor.copy(alpha = 0.22f) else when (role) {
        UserRole.BUSINESS_HEAD -> Color(0xFFE3F2FD)
        UserRole.MANAGER -> Color(0xFFE3F2FD)
        UserRole.APPROVER -> Color(0xFFE8F5E9)
        UserRole.ADMIN -> Color(0xFFF3E5F5)
        UserRole.USER -> Color(0xFFE1F5FE)
    }

    val displayName = name.ifBlank { "—" }

    val initials = displayName
        .trim()
        .split("\\s+".toRegex())
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); visible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontFamily = iosFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tier2Surface,
                    titleContentColor = primaryTextColor,
                    navigationIconContentColor = Color(0xFF007AFF)
                )
            )
        },
        containerColor = tier1Background
    ) { paddingValues ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF007AFF))
            }
            return@Scaffold
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 8 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // ── Avatar ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(
                            elevation = if (isDarkMode) 0.dp else 8.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFF007AFF).copy(alpha = 0.15f),
                            spotColor   = Color(0xFF007AFF).copy(alpha = 0.25f)
                        )
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF007AFF), Color(0xFF0051CA))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = iosFont,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Name ────────────────────────────────────────────────────
                Text(
                    text = displayName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = iosFont,
                    color = primaryTextColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Role badge ──────────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(50),
                    color = roleBg,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Text(
                        text = roleLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = roleColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Primary info card ───────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = tier2Surface,
                    shadowElevation = if (isDarkMode) 0.dp else 2.dp,
                    border = if (isDarkMode) BorderStroke(1.dp, dividerColor) else null
                ) {
                    Column {
                        ProfileInfoRow(
                            icon = Icons.Default.Person,
                            iconTint = Color(0xFF007AFF),
                            iconBg = Color(0xFFE3F0FF),
                            label = "Full Name",
                            value = displayName,
                            fontFamily = iosFont,
                            showDivider = true
                        )
                        ProfileInfoRow(
                            icon = Icons.Default.Phone,
                            iconTint = Color(0xFF34C759),
                            iconBg = Color(0xFFE6F9EC),
                            label = "Phone Number",
                            value = phone.ifBlank { "—" },
                            fontFamily = iosFont,
                            showDivider = !email.isNullOrBlank() || !department.isNullOrBlank() || !businessName.isNullOrBlank()
                        )
                        if (!email.isNullOrBlank()) {
                            ProfileInfoRow(
                                icon = Icons.Default.Badge,
                                iconTint = Color(0xFFFF9500),
                                iconBg = Color(0xFFFFF3E0),
                                label = "Email",
                                value = email,
                                fontFamily = iosFont,
                                showDivider = !department.isNullOrBlank() || !businessName.isNullOrBlank()
                            )
                        }
                        if (!businessName.isNullOrBlank()) {
                            ProfileInfoRow(
                                icon = Icons.Default.Store,
                                iconTint = Color(0xFF1565C0),
                                iconBg = Color(0xFFE3F2FD),
                                label = "Business Name",
                                value = businessName,
                                fontFamily = iosFont,
                                showDivider = !businessType.isNullOrBlank() || !location.isNullOrBlank() || !department.isNullOrBlank()
                            )
                        }
                        if (!businessType.isNullOrBlank()) {
                            ProfileInfoRow(
                                icon = Icons.Default.Category,
                                iconTint = Color(0xFF6D4C41),
                                iconBg = Color(0xFFEFEBE9),
                                label = "Business Type",
                                value = businessType,
                                fontFamily = iosFont,
                                showDivider = !location.isNullOrBlank() || !department.isNullOrBlank()
                            )
                        }
                        if (!location.isNullOrBlank()) {
                            ProfileInfoRow(
                                icon = Icons.Default.LocationOn,
                                iconTint = Color(0xFF00897B),
                                iconBg = Color(0xFFE0F2F1),
                                label = "Location",
                                value = location,
                                fontFamily = iosFont,
                                showDivider = !department.isNullOrBlank()
                            )
                        }
                        if (!department.isNullOrBlank()) {
                            ProfileInfoRow(
                                icon = Icons.Default.Business,
                                iconTint = Color(0xFF9C27B0),
                                iconBg = Color(0xFFF3E5F5),
                                label = "Department",
                                value = department,
                                fontFamily = iosFont,
                                showDivider = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Role card ───────────────────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = tier2Surface,
                    shadowElevation = if (isDarkMode) 0.dp else 2.dp,
                    border = if (isDarkMode) BorderStroke(1.dp, dividerColor) else null
                ) {
                    Column {
                        ProfileInfoRow(
                            icon = Icons.Default.Shield,
                            iconTint = roleColor,
                            iconBg = roleBg,
                            label = "Role",
                            value = roleLabel,
                            fontFamily = iosFont,
                            showDivider = isActive != null
                        )
                        if (isActive != null) {
                            ProfileInfoRow(
                                icon = if (isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                iconTint = if (isActive) Color(0xFF43A047) else Color(0xFFE53935),
                                iconBg   = if (isActive) Color(0xFFE8F5E9)  else Color(0xFFFFEBEE),
                                label = "Account Status",
                                value = if (isActive) "Active" else "Inactive",
                                fontFamily = iosFont,
                                showDivider = false
                            )
                        }
                    }
                }

                // ── Join date card (optional) ────────────────────────────────
                if (joinDate != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = tier2Surface,
                        shadowElevation = if (isDarkMode) 0.dp else 2.dp,
                        border = if (isDarkMode) BorderStroke(1.dp, dividerColor) else null
                    ) {
                        ProfileInfoRow(
                            icon = Icons.Default.CalendarToday,
                            iconTint = Color(0xFF5C6BC0),
                            iconBg = Color(0xFFE8EAF6),
                            label = "Member Since",
                            value = joinDate,
                            fontFamily = iosFont,
                            showDivider = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    value: String,
    fontFamily: FontFamily,
    showDivider: Boolean
) {
    val isDarkMode = isSystemInDarkTheme()
    val primaryTextColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0x99EBEBF5) else Color(0x993C3C43)
    val dividerColor = if (isDarkMode) Color(0xFF38383A) else Color(0xFFF2F2F7)
    val resolvedIconBg = if (isDarkMode) iconTint.copy(alpha = 0.22f) else iconBg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(resolvedIconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = fontFamily,
                color = secondaryTextColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily,
                color = primaryTextColor
            )
        }
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 70.dp, end = 16.dp),
            color = dividerColor,
            thickness = 1.dp
        )
    }
}
