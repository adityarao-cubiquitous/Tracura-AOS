package com.cubiquitous.tracura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.service.BudgetValidationService

@Composable
fun ProjectBudgetSummaryCard(
    projectId: String,
    budgetSummary: Map<String, BudgetValidationService.DepartmentBudgetSummary>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Project Budget Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (budgetSummary.isEmpty()) {
                Text(
                    text = "No budget allocated for this project",
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Overall project statistics
                val totalAllocated = budgetSummary.values.sumOf { it.allocatedBudget }
                val totalSpent = budgetSummary.values.sumOf { it.spent }
                val totalRemaining = totalAllocated - totalSpent
                val overallUsagePercentage = if (totalAllocated > 0) (totalSpent / totalAllocated) * 100 else 0.0
                
                // Overall budget overview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            overallUsagePercentage >= 100 -> Color(0xFFFFEBEE)
                            overallUsagePercentage >= 80 -> Color(0xFFFFF3E0)
                            else -> Color(0xFFE8F5E8)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Allocated",
                                fontSize = 12.sp,
                                color = Color(0xFF7F8C8D)
                            )
                            Text(
                                text = "₹${String.format("%.2f", totalAllocated)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C3E50)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Spent",
                                fontSize = 12.sp,
                                color = Color(0xFF7F8C8D)
                            )
                            Text(
                                text = "₹${String.format("%.2f", totalSpent)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C3E50)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Remaining",
                                fontSize = 12.sp,
                                color = Color(0xFF7F8C8D)
                            )
                            Text(
                                text = "₹${String.format("%.2f", totalRemaining)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    totalRemaining < 0 -> Color(0xFFD32F2F)
                                    totalRemaining < totalAllocated * 0.1 -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Overall progress bar
                        LinearProgressIndicator(
                            progress = (overallUsagePercentage / 100).toFloat().coerceAtMost(1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = when {
                                overallUsagePercentage >= 100 -> Color(0xFFD32F2F)
                                overallUsagePercentage >= 80 -> Color(0xFFFF9800)
                                else -> Color(0xFF4CAF50)
                            },
                            trackColor = Color(0xFFE0E0E0)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "${String.format("%.1f", overallUsagePercentage)}% of total budget used",
                            fontSize = 12.sp,
                            color = Color(0xFF7F8C8D),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Department breakdown
                Text(
                    text = "Department Breakdown",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(budgetSummary.toList()) { (department, summary) ->
                        DepartmentBudgetSummaryItem(
                            department = department,
                            summary = summary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DepartmentBudgetSummaryItem(
    department: String,
    summary: BudgetValidationService.DepartmentBudgetSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                summary.percentage >= 100 -> Color(0xFFFFEBEE)
                summary.percentage >= 80 -> Color(0xFFFFF3E0)
                else -> Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = department,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                // Status icon
                Icon(
                    imageVector = when {
                        summary.percentage >= 100 -> Icons.Default.Warning
                        summary.percentage >= 80 -> Icons.Default.TrendingUp
                        else -> Icons.Default.TrendingDown
                    },
                    contentDescription = null,
                    tint = when {
                        summary.percentage >= 100 -> Color(0xFFD32F2F)
                        summary.percentage >= 80 -> Color(0xFFFF9800)
                        else -> Color(0xFF4CAF50)
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Budget details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Allocated",
                        fontSize = 10.sp,
                        color = Color(0xFF7F8C8D)
                    )
                    Text(
                        text = "₹${String.format("%.2f", summary.allocatedBudget)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spent",
                        fontSize = 10.sp,
                        color = Color(0xFF7F8C8D)
                    )
                    Text(
                        text = "₹${String.format("%.2f", summary.spent)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remaining",
                        fontSize = 10.sp,
                        color = Color(0xFF7F8C8D)
                    )
                    Text(
                        text = "₹${String.format("%.2f", summary.remaining)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            summary.remaining < 0 -> Color(0xFFD32F2F)
                            summary.remaining < summary.allocatedBudget * 0.1 -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = (summary.percentage / 100).toFloat().coerceAtMost(1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = when {
                    summary.percentage >= 100 -> Color(0xFFD32F2F)
                    summary.percentage >= 80 -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                },
                trackColor = Color(0xFFE0E0E0)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${String.format("%.1f", summary.percentage)}% used",
                fontSize = 10.sp,
                color = Color(0xFF7F8C8D),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BudgetAlertCard(
    budgetSummary: Map<String, BudgetValidationService.DepartmentBudgetSummary>,
    modifier: Modifier = Modifier
) {
    val overBudgetDepartments = budgetSummary.values.filter { it.remaining < 0 }
    val nearBudgetDepartments = budgetSummary.values.filter { 
        it.remaining >= 0 && it.percentage >= 80 && it.percentage < 100 
    }
    
    if (overBudgetDepartments.isNotEmpty() || nearBudgetDepartments.isNotEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (overBudgetDepartments.isNotEmpty()) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (overBudgetDepartments.isNotEmpty()) Color(0xFFD32F2F) else Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = if (overBudgetDepartments.isNotEmpty()) "Budget Alerts" else "Budget Warnings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (overBudgetDepartments.isNotEmpty()) Color(0xFFD32F2F) else Color(0xFFFF9800)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (overBudgetDepartments.isNotEmpty()) {
                    Text(
                        text = "Departments over budget:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD32F2F)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    overBudgetDepartments.forEach { dept ->
                        Text(
                            text = "• ${budgetSummary.entries.find { it.value == dept }?.key ?: "Unknown"}: ₹${String.format("%.2f", dept.remaining)} over budget",
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                if (nearBudgetDepartments.isNotEmpty()) {
                    if (overBudgetDepartments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Text(
                        text = "Departments near budget limit:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF9800)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    nearBudgetDepartments.forEach { dept ->
                        Text(
                            text = "• ${budgetSummary.entries.find { it.value == dept }?.key ?: "Unknown"}: ${String.format("%.1f", dept.percentage)}% used",
                            fontSize = 12.sp,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}






