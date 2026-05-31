package com.cubiquitous.tracura.ui.view.auth

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.CustomerSelectionItem
import com.cubiquitous.tracura.viewmodel.AuthViewModel

// iOS-style color palette
private data class CustomerSelectionPalette(
    val tier1Background: Color,
    val tier2Surface: Color,
    val tier3Field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val hairline: Color,
    val success: Color
)

@Composable
private fun rememberCustomerSelectionPalette(accentColor: Color = MaterialTheme.colorScheme.primary): CustomerSelectionPalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, accentColor) {
        CustomerSelectionPalette(
            tier1Background = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
            tier2Surface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            tier3Field = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF8F9FA),
            textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43),
            accent = accentColor,
            onAccent = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF),
            hairline = if (isDark) Color(0xFF38383A) else Color(0x1F000000),
            success = Color(0xFF34C759)
        )
    }
}

@Composable
fun CustomerSelectionScreen(
    onCustomerSelected: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val iosPalette = rememberCustomerSelectionPalette()
    val items by authViewModel.customerSelectionItems.collectAsState()
    val isLoading by authViewModel.isLoadingCustomerSelection.collectAsState()
    val error by authViewModel.customerSelectionError.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.loadCustomerSelections()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        iosPalette.tier1Background,
                        iosPalette.tier1Background,
                        iosPalette.tier2Surface.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(
                        color = iosPalette.accent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Loading Workspaces",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iosPalette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please wait while we fetch your workspaces...",
                        fontSize = 14.sp,
                        color = iosPalette.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Error icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 40.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Unable to Load Workspaces",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = iosPalette.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = error ?: "An error occurred while loading your workspaces. Please try again.",
                        fontSize = 15.sp,
                        color = iosPalette.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { authViewModel.loadCustomerSelections() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iosPalette.accent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = "Retry",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iosPalette.onAccent
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header section with gradient background
                    Surface(
                        color = iosPalette.tier2Surface,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 32.dp)
                        ) {
                            // Welcome icon
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(iosPalette.accent.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = iosPalette.accent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text(
                                text = "Select Workspace",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = iosPalette.textPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Choose your workspace to continue",
                                fontSize = 16.sp,
                                color = iosPalette.textSecondary,
                                lineHeight = 24.sp
                            )
                            
                            if (items.size > 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = iosPalette.tier3Field,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${items.size} workspaces available",
                                        fontSize = 13.sp,
                                        color = iosPalette.textSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Workspace list
                    if (items.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📂",
                                fontSize = 64.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Workspaces Found",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iosPalette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Contact your administrator to get access to a workspace.",
                                fontSize = 14.sp,
                                color = iosPalette.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items, key = { it.customerId }) { item ->
                                CustomerSelectionCard(
                                    item = item,
                                    palette = iosPalette,
                                    onClick = {
                                        val success = authViewModel.selectCustomer(item.customerId)
                                        if (success) {
                                            onCustomerSelected()
                                        }
                                    }
                                )
                            }
                            
                            // Bottom spacing
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerSelectionCard(
    item: CustomerSelectionItem,
    palette: CustomerSelectionPalette,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 2.dp,
        animationSpec = tween(durationMillis = 150),
        label = "CardElevation"
    )
    
    Card(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = palette.tier2Surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(1.dp, palette.hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                palette.accent.copy(alpha = 0.15f),
                                palette.accent.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.businessName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    letterSpacing = (-0.2).sp
                )
                
                if (item.businessType.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.businessType,
                        fontSize = 14.sp,
                        color = palette.textSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Role badge
                Surface(
                    color = palette.accent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.role.name.replace("_", " "),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Arrow icon
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Select",
                tint = palette.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
