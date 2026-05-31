package com.cubiquitous.tracura.ui.components

import android.R.id.progress
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
fun BudgetWarningComponent(
    budgetValidationResult: BudgetValidationService.BudgetValidationResult?,
    modifier: Modifier = Modifier
) {
    budgetValidationResult?.let { result ->
        if (!result.isValid && result.warningMessage != null) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE) // Light red background
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Budget Warning",
                        tint = Color(0xFFD32F2F), // Red color
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Budget Exceeded",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = result.warningMessage,
                            fontSize = 14.sp,
                            color = Color(0xFF424242),
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Budget details
                        BudgetDetailsRow(
                            label = "Department Budget",
                            amount = result.departmentBudget
                        )
                        
                        BudgetDetailsRow(
                            label = "Already Spent",
                            amount = result.currentSpent
                        )
                        
                        BudgetDetailsRow(
                            label = "Remaining Budget",
                            amount = result.remainingBudget,
                            isRemaining = true
                        )
                        
                        BudgetDetailsRow(
                            label = "New Expense Amount",
                            amount = result.newExpenseAmount,
                            isNewExpense = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetDetailsRow(
    label: String,
    amount: Double,
    isRemaining: Boolean = false,
    isNewExpense: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
        
        Text(
            text = "₹${String.format("%.2f", amount)}",
            fontSize = 12.sp,
            fontWeight = if (isRemaining || isNewExpense) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isRemaining -> if (amount < 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                isNewExpense -> Color(0xFFD32F2F)
                else -> Color(0xFF424242)
            }
        )
    }
}

@Composable
fun BudgetInfoCard(
    departmentBudget: Double,
    currentSpent: Double,
    remainingBudget: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA) // Light gray background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Calculate progress and color outside the Row
            val progress = if (departmentBudget > 0) (currentSpent / departmentBudget).toFloat() else 0f
            val progressColor = when {
                progress >= 1.0f -> Color(0xFFD32F2F)
                progress >= 0.8f -> Color(0xFFFF9800)
                else -> Color(0xFF4CAF50)
            }
            
            // Compact header with progress indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Status",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
                
                Text(
                    text = "${String.format("%.0f", progress * 100)}% used",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = progressColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Compact progress bar
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = progressColor,
                trackColor = Color(0xFFE0E0E0)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Compact budget details in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Allocated
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Allocated",
                        fontSize = 10.sp,
                        color = Color(0xFF7F8C8D)
                    )
                    Text(
                        text = "₹${String.format("%.0f", departmentBudget)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2C3E50)
                    )
                }
                
                // Spent
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Spent",
                        fontSize = 10.sp,
                        color = Color(0xFF7F8C8D)
                    )
                    Text(
                        text = "₹${String.format("%.0f", currentSpent)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7F8C8D)
                    )
                }
                
                // Remaining
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Remaining",
                        fontSize = 10.sp,
                        color = Color(0xFF7F8C8D)
                    )
                    Text(
                        text = "₹${String.format("%.0f", remainingBudget)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            remainingBudget < 0 -> Color(0xFFD32F2F)
                            remainingBudget < departmentBudget * 0.1 -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetExceededDialog(
    budgetValidationResult: BudgetValidationService.BudgetValidationResult,
    onDismiss: () -> Unit,
    onProceed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (budgetValidationResult.exceedsHardLimit) "Budget Blocked" else "Budget Exceeded",
                    color = Color(0xFFD32F2F)
                )
                if (budgetValidationResult.exceedsHardLimit) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFD32F2F).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "LOCKED",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        },
        text = {
            Column {
                Text(
                    text = budgetValidationResult.warningMessage ?: "This expense would exceed the department's budget.",
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Budget Details:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        BudgetDetailsRow(
                            label = "Department Budget",
                            amount = budgetValidationResult.departmentBudget
                        )
                        
                        BudgetDetailsRow(
                            label = "Already Spent",
                            amount = budgetValidationResult.currentSpent
                        )
                        
                        BudgetDetailsRow(
                            label = "Remaining Budget",
                            amount = budgetValidationResult.remainingBudget,
                            isRemaining = true
                        )
                        
                        BudgetDetailsRow(
                            label = "New Expense Amount",
                            amount = budgetValidationResult.newExpenseAmount,
                            isNewExpense = true
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!budgetValidationResult.exceedsHardLimit) {
                    // Message about Production Head approval - only if not blocked
                    Text(
                        text = "This expense will be approved by Production head",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Informative message for blocked expenses
                    Text(
                        text = "This expenditure is strictly blocked as it exceeds 150% of allocated usage.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (!budgetValidationResult.exceedsHardLimit) {
                TextButton(
                    onClick = onProceed,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("Proceed Anyway")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
