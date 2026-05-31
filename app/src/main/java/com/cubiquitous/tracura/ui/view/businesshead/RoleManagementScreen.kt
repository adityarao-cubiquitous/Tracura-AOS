package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleManagementScreen(
    onNavigateBack: () -> Unit
) {
    val palette = businessHeadUiPalette()
    val roles = listOf(
        RoleInfo(
            role = UserRole.USER,
            name = "User",
            icon = Icons.Default.Person,
            iconColor = palette.approverText,
            iconBackgroundColor = palette.approverContainer,
            description = "Standard user with basic permissions to submit and track expenses",
            permissions = listOf(
                "Submit expenses",
                "View own submissions",
                "Track expense status",
                "Upload receipts",
                "Edit pending expenses"
            )
        ),
        RoleInfo(
            role = UserRole.APPROVER,
            name = "Approver",
            icon = Icons.Default.Check,
            iconColor = palette.userText,
            iconBackgroundColor = palette.userContainer,
            description = "Can review and approve/reject expense submissions",
            permissions = listOf(
                "All User permissions",
                "Review pending expenses",
                "Approve/Reject expenses",
                "Add comments to expenses",
                "View project reports",
                "Chat with users about expenses"
            )
        ),
        RoleInfo(
            role = UserRole.BUSINESS_HEAD,
            name = "Production Head",
            icon = Icons.Default.Star,
            iconColor = palette.bhText,
            iconBackgroundColor = palette.bhContainer,
            description = "Full administrative access to manage projects, users, and approvals",
            permissions = listOf(
                "All Approver permissions",
                "Create new projects",
                "Manage users and roles",
                "Create new users",
                "View all projects",
                "Access overall reports",
                "Delegate approvals",
                "Manage project settings"
            )
        ),
        RoleInfo(
            role = UserRole.MANAGER,
            name = "Account Manager",
            icon = Icons.Default.ManageAccounts,
            iconColor = palette.managerText,
            iconBackgroundColor = palette.managerContainer,
            description = "Manage assigned projects, approve expenses, and has full project dashboard access. No department restriction.",
            permissions = listOf(
                "Manage and oversee assigned projects",
                "Approve and review project expenses",
                "Full project dashboard access",
                "No department restriction"
            )
        ),
        RoleInfo(
            role = UserRole.ADMIN,
            name = "Administrator",
            icon = Icons.Default.Settings,
            iconColor = palette.adminText,
            iconBackgroundColor = palette.adminContainer,
            description = "System administrator with complete control over the application",
            permissions = listOf(
                "Complete system access",
                "Manage all users",
                "Manage all projects",
                "System configuration",
                "Access to all reports",
                "Security management"
            )
        )
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Role Management",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.tier2Surface,
                    titleContentColor = palette.primaryText,
                    navigationIconContentColor = palette.accent
                )
            )
        },
        containerColor = palette.tier1Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Description header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = palette.approverContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "View and understand different user roles and their permissions in the system",
                        fontSize = 14.sp,
                        color = palette.accent,
                        lineHeight = 20.sp
                    )
                }
            }
            
            // Roles list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(roles) { roleInfo ->
                    RoleCard(roleInfo = roleInfo, palette = palette)
                }
            }
        }
    }
}

@Composable
private fun RoleCard(roleInfo: RoleInfo, palette: BusinessHeadUiPalette) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = palette.tier2Surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Role header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(roleInfo.iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = roleInfo.icon,
                        contentDescription = null,
                        tint = roleInfo.iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Role name
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = roleInfo.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = roleInfo.description,
                        fontSize = 14.sp,
                        color = palette.secondaryText,
                        lineHeight = 20.sp
                    )
                }
                
                // Expand/collapse button
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = palette.secondaryText
                    )
                }
            }
            
            // Permissions list (expandable)
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = palette.outline)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Permissions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                roleInfo.permissions.forEach { permission ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = roleInfo.iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = permission,
                            fontSize = 14.sp,
                            color = palette.primaryText,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

private data class RoleInfo(
    val role: UserRole,
    val name: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBackgroundColor: Color,
    val description: String,
    val permissions: List<String>
)



