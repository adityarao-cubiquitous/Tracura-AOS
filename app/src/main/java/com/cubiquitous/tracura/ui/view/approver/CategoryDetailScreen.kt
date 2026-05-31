package com.cubiquitous.tracura.ui.view.approver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.DetailedExpense
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    projectId: String,
    categoryName: String,
    onNavigateBack: () -> Unit,
    reportsViewModel: ReportsViewModel = hiltViewModel()
) {
    val reportData by reportsViewModel.reportData.collectAsState()
    val isLoading by reportsViewModel.isLoading.collectAsState()
    
    // Filter expenses for this specific category
    val categoryExpenses = remember(reportData.detailedExpenses, categoryName) {
        reportData.detailedExpenses.filter { expense ->
            expense.department.equals(categoryName, ignoreCase = true) ||
            reportData.expensesByCategory.contains(categoryName)
        }
    }
    
    val categoryAmount = reportData.expensesByCategory[categoryName] ?: 0.0
    
    LaunchedEffect(projectId) {
        if (reportData.detailedExpenses.isEmpty()) {
            reportsViewModel.loadReportsForProject(projectId)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Category Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = getAttractiveCategoryColor(categoryName)
            )
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF8B5FBF))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = getAttractiveCategoryColor(categoryName)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = categoryName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Total Spent",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            
                            Text(
                                text = FormatUtils.formatCurrency(categoryAmount),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "${categoryExpenses.size} expenses",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                // Expenses List Header
                item {
                    Text(
                        text = "Expense Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                if (categoryExpenses.isNotEmpty()) {
                    items(categoryExpenses) { expense ->
                        ExpenseDetailCard(expense = expense)
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🔍 No Expenses Found",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No expenses found for this category",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseDetailCard(expense: DetailedExpense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Use remainingBalance if not null and >= 1, otherwise use amount
                    val displayAmount = if (expense.remainingBalance != null && expense.remainingBalance!! >= 1) {
                        expense.remainingBalance!!
                    } else {
                        expense.amount
                    }
                    Text(
                        text = FormatUtils.formatCurrency(displayAmount),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Text(
                        text = expense.by.ifEmpty { "Deeksha" },
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = expense.date?.let { FormatUtils.formatDate(it) } ?: "No date",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    
                    Text(
                        text = expense.department.ifEmpty { "No Department" },
                        fontSize = 12.sp,
                        color = Color(0xFF4285F4),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (expense.invoice.isNotEmpty() && expense.invoice != "N/A") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Invoice: ${expense.invoice}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            if (expense.modeOfPayment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Payment: ${expense.modeOfPayment}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// More attractive gradient-like colors for category cards (duplicate function for this screen)
private fun getAttractiveCategoryColor(categoryName: String): Color {
    val attractiveColors = listOf(
        Color(0xFF6366F1), // Modern Indigo
        Color(0xFF10B981), // Emerald Green
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Violet
        Color(0xFF06B6D4), // Cyan
        Color(0xFFEC4899), // Pink
        Color(0xFF84CC16), // Lime
        Color(0xFFF97316), // Orange
        Color(0xFF3B82F6), // Blue
        Color(0xFF14B8A6), // Teal
        Color(0xFFA855F7)  // Purple
    )
    
    // Use category name hash to consistently assign the same color to the same category
    val colorIndex = kotlin.math.abs(categoryName.hashCode()) % attractiveColors.size
    return attractiveColors[colorIndex]
} 