package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.R

private data class IosUserMgmtPalette(
    val tier1Background: Color,
    val tier2Surface: Color,
    val tier3Field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val hairline: Color
)

@Composable
private fun rememberIosUserMgmtPalette(accentColor: Color = MaterialTheme.colorScheme.primary): IosUserMgmtPalette {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, accentColor) {
        IosUserMgmtPalette(
            tier1Background = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
            tier2Surface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            tier3Field = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
            textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43),
            accent = accentColor,
            hairline = if (isDark) Color(0xFF38383A) else Color(0x1F000000)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessHeadUserManagementTab(
    onNavigateToCreateUser: () -> Unit,
    onNavigateToViewAllUsers: () -> Unit,
    onNavigateToDepartmentUserManagement: () -> Unit,
    onNavigateToRoleManagement: () -> Unit,
    onLogout: () -> Unit
) {
    val iosPalette = rememberIosUserMgmtPalette()
    var showVendorDetailsSheet by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iosPalette.tier1Background)
    ) {
        // iOS-style header - Title at top only
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "User Management",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = iosPalette.textPrimary
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Large icon below the title - Three person silhouettes
            Box(
                //modifier = Modifier
                    //.size(120.dp)
                    /*.clip(CircleShape)
                    .background.Transparent,*/
                contentAlignment = Alignment.Center
            ) {
                // Three person silhouettes - one solid, two outlined
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.user),
                        contentDescription = null,
                        tint = iosPalette.accent,
                        modifier = Modifier.size(120.dp)
                    )
                }
            }
            
            //Spacer(modifier = Modifier.height(4.dp))
            
            // Subtitle under the icon - Bold, centered
            Text(
                text = "User Management",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = iosPalette.textPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Description - Medium-light gray, smaller, centered
            Text(
                text = "Manage users, roles, and permissions",
                fontSize = 20.sp,
                color = iosPalette.textSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(36.dp))
            
            // Management options cards with increased spacing
            UserManagementCard(
                painter = painterResource(id = R.drawable.createnewuser),
                iconColor = iosPalette.accent,
                iconBackgroundColor = iosPalette.tier3Field,
                title = "Create New User",
                description = "Add a new user to the system with appropriate role",
                onClick = onNavigateToCreateUser
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            UserManagementCard(
                painter = painterResource(id = R.drawable.viewalluser),
                iconColor = iosPalette.accent,
                iconBackgroundColor = iosPalette.tier3Field,
                title = "View All Users",
                description = "Browse and manage existing users",
                onClick = onNavigateToViewAllUsers
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            UserManagementCard(
                painter = painterResource(id = R.drawable.usermanagement),
                iconColor = iosPalette.accent,
                iconBackgroundColor = iosPalette.tier3Field,
                title = "Department User Management",
                description = "Assign or replace users and approvers by department",
                onClick = onNavigateToDepartmentUserManagement
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            UserManagementCard(
                painter = painterResource(id = R.drawable.rolemanagement),
                iconColor = iosPalette.accent,
                iconBackgroundColor = iosPalette.tier3Field,
                title = "Role Management",
                description = "Configure user roles and permissions",
                onClick = onNavigateToRoleManagement
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            UserManagementCard(
                painter = painterResource(id = R.drawable.user),
                iconColor = iosPalette.accent,
                iconBackgroundColor = iosPalette.tier3Field,
                title = "Vendor Management",
                description = "Manage vendors by department",
                onClick = { showVendorDetailsSheet = true }
            )
        }
    }
    
    // Vendor Details Modal Sheet
    if (showVendorDetailsSheet) {
        VendorDetailsModalSheet(
            onDismiss = { showVendorDetailsSheet = false }
        )
    }
}

@Composable
private fun UserManagementCard(
    painter : Painter,
    iconColor: Color,
    iconBackgroundColor: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val iosPalette = rememberIosUserMgmtPalette()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isSystemInDarkTheme()) Color.Transparent else Color(0x14000000)
            ),
        colors = CardDefaults.cardColors(
            containerColor = iosPalette.tier2Surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, iosPalette.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with color background circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = iosPalette.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = iosPalette.textSecondary
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Thin, soft gray arrow icon
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = iosPalette.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
