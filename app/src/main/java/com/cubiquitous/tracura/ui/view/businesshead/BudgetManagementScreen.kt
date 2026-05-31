package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.ui.components.ProjectBudgetSummaryCard
import com.cubiquitous.tracura.ui.components.BudgetAlertCard
import com.cubiquitous.tracura.viewmodel.BudgetSummaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetManagementScreen(
    projectId: String,
    projectName: String,
    onNavigateBack: () -> Unit,
    onNavigateToProjectSettings: () -> Unit,
    budgetSummaryViewModel: BudgetSummaryViewModel = hiltViewModel()
) {
    val budgetSummary by budgetSummaryViewModel.budgetSummary.collectAsState()
    val isLoading by budgetSummaryViewModel.isLoading.collectAsState()
    val error by budgetSummaryViewModel.error.collectAsState()
    val totalAllocated by budgetSummaryViewModel.totalAllocated.collectAsState()
    val totalSpent by budgetSummaryViewModel.totalSpent.collectAsState()
    val totalRemaining by budgetSummaryViewModel.totalRemaining.collectAsState()
    val overallUsagePercentage by budgetSummaryViewModel.overallUsagePercentage.collectAsState()
    val hasAlerts by budgetSummaryViewModel.hasAlerts.collectAsState()
    
    // Load budget summary when screen opens
    LaunchedEffect(projectId) {
        budgetSummaryViewModel.loadBudgetSummary(projectId)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Budget Management",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = projectName,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = { budgetSummaryViewModel.refreshBudgetSummary(projectId) }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onNavigateToProjectSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Project Settings",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2C3E50)
            )
        )
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF2C3E50)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading budget data...",
                            color = Color(0xFF7F8C8D)
                        )
                    }
                }
            }
            
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading budget data",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "Unknown error",
                            color = Color(0xFF7F8C8D),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { budgetSummaryViewModel.refreshBudgetSummary(projectId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2C3E50)
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Stats Card
                    item {
                        QuickStatsCard(
                            totalAllocated = totalAllocated,
                            totalSpent = totalSpent,
                            totalRemaining = totalRemaining,
                            usagePercentage = overallUsagePercentage
                        )
                    }
                    
                    // Budget Alerts
                    if (hasAlerts) {
                        item {
                            BudgetAlertCard(
                                budgetSummary = budgetSummary
                            )
                        }
                    }
                    
                    // Detailed Budget Summary
                    item {
                        ProjectBudgetSummaryCard(
                            projectId = projectId,
                            budgetSummary = budgetSummary
                        )
                    }
                    
                    // Department Management Actions
                    item {
                        DepartmentManagementCard(
                            onManageDepartments = onNavigateToProjectSettings
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatsCard(
    totalAllocated: Double,
    totalSpent: Double,
    totalRemaining: Double,
    usagePercentage: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                usagePercentage >= 100 -> Color(0xFFFFEBEE)
                usagePercentage >= 80 -> Color(0xFFFFF3E0)
                else -> Color(0xFFE8F5E8)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Quick Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total Budget",
                    value = "₹${String.format("%.2f", totalAllocated)}",
                    color = Color(0xFF2C3E50)
                )
                
                StatItem(
                    label = "Spent",
                    value = "₹${String.format("%.2f", totalSpent)}",
                    color = Color(0xFF7F8C8D)
                )
                
                StatItem(
                    label = "Remaining",
                    value = "₹${String.format("%.2f", totalRemaining)}",
                    color = when {
                        totalRemaining < 0 -> Color(0xFFD32F2F)
                        totalRemaining < totalAllocated * 0.1 -> Color(0xFFFF9800)
                        else -> Color(0xFF4CAF50)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Usage percentage
            Text(
                text = "Budget Usage: ${String.format("%.1f", usagePercentage)}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = (usagePercentage / 100).toFloat().coerceAtMost(1f),
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    usagePercentage >= 100 -> Color(0xFFD32F2F)
                    usagePercentage >= 80 -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                },
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF7F8C8D)
        )
    }
}

@Composable
private fun DepartmentManagementCard(
    onManageDepartments: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Department Management",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Manage department budgets and allocations for this project.",
                fontSize = 14.sp,
                color = Color(0xFF7F8C8D)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onManageDepartments,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C3E50)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Manage Department Budgets",
                    color = Color.White
                )
            }
        }
    }
}
