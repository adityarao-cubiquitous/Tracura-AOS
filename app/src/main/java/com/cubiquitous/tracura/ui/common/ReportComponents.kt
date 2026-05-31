package com.cubiquitous.tracura.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.CategoryData
import com.cubiquitous.tracura.model.FilterOptions
import com.cubiquitous.tracura.viewmodel.DetailedExpenseWithProject
import com.cubiquitous.tracura.viewmodel.OverallReportsViewModel
import com.cubiquitous.tracura.viewmodel.ProjectFilter
import com.cubiquitous.tracura.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverallReportsContent(
    title: String,
    primaryColor: Color,
    onNavigateBack: () -> Unit,
    overallReportsViewModel: OverallReportsViewModel,
    onExportPDF: () -> Unit,
    onExportCSV: () -> Unit
) {
    val reportData by overallReportsViewModel.reportData.collectAsState()
    val isLoading by overallReportsViewModel.isLoading.collectAsState()
    val error by overallReportsViewModel.error.collectAsState()
    val selectedTimeRange by overallReportsViewModel.selectedTimeRange.collectAsState()
    val selectedProject by overallReportsViewModel.selectedProject.collectAsState()
    val isExporting by overallReportsViewModel.isExporting.collectAsState()
    
    var showTimeRangeDropdown by remember { mutableStateOf(false) }
    var showProjectDropdown by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Data loading is now handled by the parent screen
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = title,
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
            actions = {
                IconButton(
                    onClick = { overallReportsViewModel.refreshData() },
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = primaryColor
            )
        )
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryColor)
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error Loading Reports",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Text(
                            text = error ?: "Unknown error",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { 
                                overallReportsViewModel.clearError()
                                overallReportsViewModel.loadDataOnce() 
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor
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
                    // Filters Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Time Range Filter
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Time Range",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        OutlinedButton(
                                            onClick = { showTimeRangeDropdown = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = FilterOptions.timeRanges.find { it.value == selectedTimeRange }?.displayName ?: "This Year",
                                                    color = Color.Black,
                                                    fontSize = 14.sp
                                                )
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = Color.Gray
                                                )
                                            }
                                        }
                                        
                                        DropdownMenu(
                                            expanded = showTimeRangeDropdown,
                                            onDismissRequest = { showTimeRangeDropdown = false }
                                        ) {
                                            FilterOptions.timeRanges.forEach { timeRange ->
                                                DropdownMenuItem(
                                                    text = { Text(timeRange.displayName) },
                                                    onClick = {
                                                        overallReportsViewModel.updateTimeRange(timeRange.value)
                                                        showTimeRangeDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Project Filter
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Project",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        OutlinedButton(
                                            onClick = { showProjectDropdown = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = overallReportsViewModel.getProjectFilters().find { it.value == selectedProject }?.name ?: "All Projects",
                                                    color = Color.Black,
                                                    fontSize = 14.sp
                                                )
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = Color.Gray
                                                )
                                            }
                                        }
                                        
                                        DropdownMenu(
                                            expanded = showProjectDropdown,
                                            onDismissRequest = { showProjectDropdown = false }
                                        ) {
                                            overallReportsViewModel.getProjectFilters().forEach { projectFilter ->
                                                DropdownMenuItem(
                                                    text = { Text(projectFilter.name) },
                                                    onClick = {
                                                        overallReportsViewModel.updateProject(projectFilter.value)
                                                        showProjectDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Summary Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Total Spent Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Total Spent",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = FormatUtils.formatCurrency(reportData.totalSpent),
                                        fontSize = 20.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            // Budget Usage Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Budget Usage",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column {
                                        Text(
                                            text = "${String.format("%.1f", reportData.budgetUsagePercentage)}%",
                                            fontSize = 20.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (reportData.totalBudget > 0) {
                                            Text(
                                                text = "of ${FormatUtils.formatCurrency(reportData.totalBudget)}",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Category Split Chart
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Category Split",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                CategoryChart(
                                    categories = overallReportsViewModel.getCategoryData(),
                                    modifier = Modifier.fillMaxWidth().height(350.dp)
                                )
                            }
                        }
                    }
                    
                    // Detailed Expense Overview
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Detailed Expenses",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                ExpenseTable(
                                    expenses = reportData.detailedExpenses,
                                    showProject = selectedProject == "all",
                                    hasMoreExpenses = overallReportsViewModel.hasMoreExpenses.collectAsState().value,
                                    isLoadingMore = overallReportsViewModel.isLoadingMore.collectAsState().value,
                                    onLoadMore = { overallReportsViewModel.loadNextPage() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    
                    // Export Buttons (moved below detailed expenses)
                    if (reportData.detailedExpenses.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Export PDF Button
                                Button(
                                    onClick = onExportPDF,
                                    enabled = !isExporting,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2196F3)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Email,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (isExporting) "Exporting..." else "Export PDF",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                
                                // Export CSV Button
                                Button(
                                    onClick = onExportCSV,
                                    enabled = !isExporting,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Email,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (isExporting) "Exporting..." else "Export CSV",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChart(
    categories: List<CategoryData>,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No category data available",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        return
    }
    
    Column(modifier = modifier) {
        // Find the maximum spent amount for scaling
        val maxSpent = categories.maxOfOrNull { it.amount } ?: 1.0
        
        // Color palette for the bars
        val colors = listOf(
            Color(0xFF4285F4), // Blue
            Color(0xFF34A853), // Green  
            Color(0xFF9C27B0), // Purple
            Color(0xFFFF9800), // Orange
            Color(0xFFF44336), // Red
            Color(0xFF607D8B)  // Blue Grey
        )
        
        // Create vertical bar chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            categories.forEachIndexed { index, category ->
                val spent = category.amount
                val barHeight = if (maxSpent > 0) (spent / maxSpent) else 0.0
                val barColor = colors[index % colors.size]
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Amount text at top
                    Text(
                        text = FormatUtils.formatCurrency(spent),
                        fontSize = 10.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.height(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Vertical bar
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height((200 * barHeight).dp)
                            .background(barColor, RoundedCornerShape(8.dp))
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Category name at bottom
                    Text(
                        text = category.category,
                        fontSize = 10.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.height(32.dp),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseTable(
    expenses: List<DetailedExpenseWithProject>,
    showProject: Boolean,
    hasMoreExpenses: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Date",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            if (showProject) {
                Text(
                    text = "Project",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "Invoice",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "By",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Amount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Text(
                text = "Dept",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
        
        // Table Rows
        expenses.forEach { expense ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = expense.date?.toDate()?.let { 
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) 
                    } ?: "N/A",
                    fontSize = 12.sp,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                if (showProject) {
                    Text(
                        text = expense.projectName,
                        fontSize = 12.sp,
                        color = Color(0xFF8B5FBF),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = expense.invoice,
                    fontSize = 12.sp,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = expense.by,
                    fontSize = 12.sp,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = FormatUtils.formatCurrency(expense.amount),
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Text(
                    text = expense.department,
                    fontSize = 12.sp,
                    color = Color(0xFF4285F4),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
            
            if (expense != expenses.last()) {
                HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
            }
        }
        
        // Load More Button
        if (hasMoreExpenses) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onLoadMore,
                enabled = !isLoadingMore,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5FBF)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoadingMore) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Text(
                            text = "Loading more expenses...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Load More Expenses",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // Show total count if there are expenses
        if (expenses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Showing ${expenses.size} expenses",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
} 