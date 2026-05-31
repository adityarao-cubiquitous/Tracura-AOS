package com.cubiquitous.tracura.ui.view.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToManageUsers: () -> Unit,
    onNavigateToManageProjects: () -> Unit,
    onNavigateToReports: () -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    
    // Refresh user data when screen opens to ensure current user is loaded
    LaunchedEffect(Unit) {
        authViewModel.refreshUserData()
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Admin Dashboard") },
            actions = {
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                }
            }
        )
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Admin Dashboard",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(onClick = onNavigateToManageUsers) {
                    Text("Manage Users")
                }
                
                Button(onClick = onNavigateToManageProjects) {
                    Text("Manage Projects")
                }
                
                Button(onClick = onNavigateToReports) {
                    Text("View Reports")
                }
            }
        }
    }
} 