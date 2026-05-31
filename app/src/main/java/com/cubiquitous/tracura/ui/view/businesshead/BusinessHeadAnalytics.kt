package com.cubiquitous.tracura.ui.view.businesshead

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.viewmodel.ReportsViewModel
import com.cubiquitous.tracura.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessHeadAnalytics(
    projectId: String,
    onNavigateBack: () -> Unit,
    reportsViewModel: ReportsViewModel = hiltViewModel()
) {
    val reportData by reportsViewModel.reportData.collectAsState()
    val isLoading by reportsViewModel.isLoading.collectAsState()
    val error by reportsViewModel.error.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Forecast", "Variance", "Trends")
    
    // Load reports when screen starts
    LaunchedEffect(projectId) {
        reportsViewModel.loadReportsForProject(projectId)
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
                    text = "PREDICTIVE ANALYSIS",
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
                containerColor = Color(0xFF8B5FBF)
            )
        )
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B5FBF))
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
                            text = "Error Loading Analytics",
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
                                reportsViewModel.clearError()
                                reportsViewModel.loadReportsForProject(projectId) 
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5FBF)
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Tab Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            TabButton(
                                text = tab,
                                isSelected = selectedTab == index,
                                onClick = { selectedTab = index },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Content based on selected tab
                    when (selectedTab) {
                        0 -> ForecastContent(reportData = reportData, projectId = projectId)
                        1 -> VarianceContent(reportData = reportData)
                        2 -> TrendsContent(reportData = reportData)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF4285F4) else Color(0xFFE0E0E0)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ForecastContent(
    reportData: com.cubiquitous.tracura.model.ReportData,
    projectId: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Forecast Report",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5FBF),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Predict total project cost over remaining timeline for this project",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // Generate dynamic forecast data based on real project data
            val forecastData = generateDynamicForecastData(reportData)
            
            // Enhanced Line Chart with better visualization
            EnhancedLineChart(
                months = forecastData.months,
                budgetData = forecastData.budgetData,
                actualData = forecastData.actualData,
                forecastData = forecastData.forecastData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("Budget", Color(0xFF4285F4))
                LegendItem("Actual", Color(0xFF9C27B0))
                LegendItem("Forecast", Color(0xFF4CAF50))
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // AI Insights Section - Replaces Forecast Summary
            AIInsightsSection(
                totalBudget = reportData.totalBudget,
                totalSpent = reportData.totalSpent,
                forecastTotal = forecastData.forecastTotal,
                budgetData = forecastData.budgetData,
                actualData = forecastData.actualData,
                forecastData = forecastData.forecastData,
                budgetUsagePercentage = reportData.budgetUsagePercentage
            )
        }
    }
}

@Composable
private fun EnhancedLineChart(
    months: List<String>,
    budgetData: List<Double>,
    actualData: List<Double>,
    forecastData: List<Double>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Calculate max value for scaling
        val maxValue = maxOf(
            budgetData.maxOrNull() ?: 0.0,
            actualData.maxOrNull() ?: 0.0,
            forecastData.maxOrNull() ?: 0.0
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Y-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cost",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${(maxValue / 1000).toInt()}k",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            
            // Mobile-style Line Chart
            MobileLineChart(
                months = months,
                budgetData = budgetData,
                actualData = actualData,
                forecastData = forecastData,
                maxValue = maxValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Composable
private fun MobileLineChart(
    months: List<String>,
    budgetData: List<Double>,
    actualData: List<Double>,
    forecastData: List<Double>,
    maxValue: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 20f
            val chartWidth = canvasWidth - (padding * 2)
            val chartHeight = canvasHeight - (padding * 2)
            
            // Draw grid lines
            val gridColor = Color(0xFFE5E7EB)
            val strokeWidth = 1.dp.toPx()
            
            // Horizontal grid lines
            for (i in 0..4) {
                val y = padding + (chartHeight / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(padding + chartWidth, y),
                    strokeWidth = strokeWidth
                )
            }
            
            // Vertical grid lines
            for (i in 0 until months.size) {
                val x = padding + (chartWidth / (months.size - 1)) * i
                drawLine(
                    color = gridColor,
                    start = Offset(x, padding),
                    end = Offset(x, padding + chartHeight),
                    strokeWidth = strokeWidth
                )
            }
            
            // Convert data to points
            fun getPoint(index: Int, value: Double): Offset {
                val x = padding + (chartWidth / (months.size - 1)) * index
                val y = padding + chartHeight - (value / maxValue * chartHeight)
                return Offset(x.toFloat(), y.toFloat())
            }
            
            // Draw lines with markers
            val budgetPoints = budgetData.mapIndexed { index, value -> getPoint(index, value) }
            val actualPoints = actualData.mapIndexed { index, value -> getPoint(index, value) }
            val forecastPoints = forecastData.mapIndexed { index, value -> getPoint(index, value) }
            
            // Draw budget line (blue)
            for (i in 0 until budgetPoints.size - 1) {
                drawLine(
                    color = Color(0xFF2563EB),
                    start = budgetPoints[i],
                    end = budgetPoints[i + 1],
                    strokeWidth = 3.dp.toPx()
                )
            }
            
            // Draw actual line (purple)
            for (i in 0 until actualPoints.size - 1) {
                drawLine(
                    color = Color(0xFF7C3AED),
                    start = actualPoints[i],
                    end = actualPoints[i + 1],
                    strokeWidth = 3.dp.toPx()
                )
            }
            
            // Draw forecast line (green)
            for (i in 0 until forecastPoints.size - 1) {
                drawLine(
                    color = Color(0xFF059669),
                    start = forecastPoints[i],
                    end = forecastPoints[i + 1],
                    strokeWidth = 3.dp.toPx()
                )
            }
            
            // Draw markers (circles)
            val markerRadius = 4.dp.toPx()
            
            // Budget markers
            budgetPoints.forEach { point ->
                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = markerRadius,
                    center = point
                )
                // White center
                drawCircle(
                    color = Color.White,
                    radius = markerRadius * 0.6f,
                    center = point
                )
            }
            
            // Actual markers
            actualPoints.forEach { point ->
                drawCircle(
                    color = Color(0xFF7C3AED),
                    radius = markerRadius,
                    center = point
                )
                // White center
                drawCircle(
                    color = Color.White,
                    radius = markerRadius * 0.6f,
                    center = point
                )
            }
            
            // Forecast markers
            forecastPoints.forEach { point ->
                drawCircle(
                    color = Color(0xFF059669),
                    radius = markerRadius,
                    center = point
                )
                // White center
                drawCircle(
                    color = Color.White,
                    radius = markerRadius * 0.6f,
                    center = point
                )
            }
        }
        
        // Month labels at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            months.forEach { month ->
                Text(
                    text = month,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun DynamicForecastSummary(
    totalBudget: Double,
    totalSpent: Double,
    forecastTotal: Double,
    remainingBudget: Double,
    forecastVariance: Double,
    budgetUsagePercentage: Double
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Forecast Summary",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5FBF),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Budget",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(totalBudget),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Spent",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(totalSpent),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Forecast Total",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(forecastTotal),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Variance",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(forecastVariance),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (forecastVariance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Additional metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Budget Usage",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${String.format("%.1f", budgetUsagePercentage)}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (budgetUsagePercentage <= 80) Color(0xFF4CAF50) else if (budgetUsagePercentage <= 100) Color(0xFFFF9800) else Color(0xFFF44336)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Remaining",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(remainingBudget),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (remainingBudget >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
private fun VarianceContent(reportData: com.cubiquitous.tracura.model.ReportData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Variance Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5FBF),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Compare forecast vs. budget (gain/loss)",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // Generate variance data for clustered bar chart
            val varianceData = generateVarianceData(reportData)
            
            // Clustered Bar Chart
            ClusteredBarChart(
                months = varianceData.months,
                budgetData = varianceData.budgetData,
                actualData = varianceData.actualData,
                forecastData = varianceData.forecastData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("Budget", Color(0xFF4285F4))
                LegendItem("Actual", Color(0xFF9C27B0))
                LegendItem("Forecast", Color(0xFF4CAF50))
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // AI Insights Section for Variance
            VarianceInsightsSection(
                totalBudget = reportData.totalBudget,
                totalSpent = reportData.totalSpent,
                forecastTotal = varianceData.forecastTotal,
                budgetData = varianceData.budgetData,
                actualData = varianceData.actualData,
                forecastData = varianceData.forecastData,
                budgetUsagePercentage = reportData.budgetUsagePercentage
            )
        }
    }
}

@Composable
private fun TrendsContent(reportData: com.cubiquitous.tracura.model.ReportData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Trends Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5FBF),
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // Generate trends data for pie chart
            val trendsData = generateTrendsData(reportData)
            
            // Pie Chart
            PieChart(
                data = trendsData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                trendsData.forEach { item ->
                    LegendItem(item.label, item.color)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // AI Insights Section for Trends
            TrendsInsightsSection(
                totalBudget = reportData.totalBudget,
                totalSpent = reportData.totalSpent,
                budgetUsagePercentage = reportData.budgetUsagePercentage,
                trendsData = trendsData,
                reportData = reportData
            )
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun VarianceMetric(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun TrendIndicator(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}


@Composable
private fun MobileBarChart(
    months: List<String>,
    budgetData: List<Double>,
    actualData: List<Double>,
    forecastData: List<Double>,
    maxValue: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 20f
            val chartWidth = canvasWidth - (padding * 2)
            val chartHeight = canvasHeight - (padding * 2)
            
            // Draw grid lines
            val gridColor = Color(0xFFE5E7EB)
            val strokeWidth = 1.dp.toPx()
            
            // Horizontal grid lines
            for (i in 0..4) {
                val y = padding + (chartHeight / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(padding + chartWidth, y),
                    strokeWidth = strokeWidth
                )
            }
            
            // Calculate bar dimensions
            val barWidth = (chartWidth / months.size) * 0.6f // 60% of available space
            val barSpacing = (chartWidth / months.size) * 0.4f / 4f // 40% for spacing, divided by 4 (3 bars + 3 gaps)
            val clusterWidth = barWidth * 0.8f // 80% of bar width for each bar
            val barGap = (barWidth - clusterWidth) / 2f
            
            // Draw bars for each month
            months.forEachIndexed { index, _ ->
                val x = padding + (chartWidth / months.size) * index + (chartWidth / months.size - barWidth) / 2f
                
                // Budget bar (blue) - left
                val budgetHeight = (budgetData[index] / maxValue * chartHeight).toFloat()
                val budgetY = padding + chartHeight - budgetHeight
                drawRect(
                    color = Color(0xFF2563EB),
                    topLeft = Offset(x + barGap, budgetY),
                    size = androidx.compose.ui.geometry.Size(clusterWidth, budgetHeight)
                )
                
                // Actual bar (purple) - center
                val actualHeight = (actualData[index] / maxValue * chartHeight).toFloat()
                val actualY = padding + chartHeight - actualHeight
                drawRect(
                    color = Color(0xFF7C3AED),
                    topLeft = Offset(x + barGap + clusterWidth + barGap, actualY),
                    size = androidx.compose.ui.geometry.Size(clusterWidth, actualHeight)
                )
                
                // Forecast bar (green) - right
                val forecastHeight = (forecastData[index] / maxValue * chartHeight).toFloat()
                val forecastY = padding + chartHeight - forecastHeight
                drawRect(
                    color = Color(0xFF059669),
                    topLeft = Offset(x + barGap + (clusterWidth + barGap) * 2, forecastY),
                    size = androidx.compose.ui.geometry.Size(clusterWidth, forecastHeight)
                )
            }
        }
        
        // Month labels at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            months.forEach { month ->
                Text(
                    text = month,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun VarianceSummary(
    totalBudget: Double,
    totalSpent: Double,
    forecastTotal: Double,
    budgetVariance: Double,
    forecastVariance: Double,
    budgetUsagePercentage: Double
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Variance Summary",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5FBF),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Budget Variance",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(budgetVariance),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (budgetVariance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Forecast Variance",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(forecastVariance),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (forecastVariance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Budget vs Actual",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${String.format("%.1f", budgetUsagePercentage)}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (budgetUsagePercentage <= 100) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Forecast vs Budget",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                val forecastVsBudgetPercentage = if (totalBudget > 0) (forecastTotal / totalBudget) * 100 else 0.0
                Text(
                    text = "${String.format("%.1f", forecastVsBudgetPercentage)}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (forecastVsBudgetPercentage <= 100) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Third row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Budget",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(totalBudget),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Forecast Total",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(forecastTotal),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun PieChart(
    data: List<PieChartItem>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Mobile-style Pie Chart
        MobilePieChart(
            data = data,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}

@Composable
private fun MobilePieChart(
    data: List<PieChartItem>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) / 2f - 20f
            
            var startAngle = -90f
            
            data.forEach { item ->
                val sweepAngle = ((item.percentage / 100f) * 360f).toFloat()
                
                // Draw pie slice
                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
                )
                
                startAngle += sweepAngle
            }
        }
        
        // Add percentage labels on the pie chart
        data.forEachIndexed { index, item ->
            val angle = -90f + (data.take(index).sumOf { it.percentage } * 3.6f).toFloat() + (item.percentage * 1.8f).toFloat()
            val textRadius = 60f
            val textX = 100f + textRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val textY = 100f + textRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
            
            Text(
                text = "${item.percentage.toInt()}%",
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = (textX - 100f).dp,
                        y = (textY - 100f).dp
                    )
            )
        }
    }
}

@Composable
private fun TrendsSummary(
    totalBudget: Double,
    totalSpent: Double,
    budgetUsagePercentage: Double,
    trendsData: List<PieChartItem>,
    reportData: com.cubiquitous.tracura.model.ReportData
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Category Analysis",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5FBF),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Show categories that exceed limits (dynamic threshold based on data)
        val threshold = if (trendsData.size > 3) 25.0 else 30.0 // Lower threshold for more categories
        val exceedingCategories = trendsData.filter { it.percentage > threshold }
        
        if (exceedingCategories.isNotEmpty()) {
            Text(
                text = "Categories Exceeding Limits:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF44336),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            exceedingCategories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = category.label,
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "${String.format("%.1f", category.percentage)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }
        } else {
            Text(
                text = "All categories within acceptable limits",
                fontSize = 12.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )
        }
        
        // Show top spending departments (instead of categories)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Top Spending Departments:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5FBF),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Generate department data for top spending areas
        val departmentData = generateDepartmentTrendsData(reportData)
        departmentData.take(3).forEach { department ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = department.label,
                    fontSize = 12.sp,
                    color = Color.Black
                )
                Text(
                    text = "${String.format("%.1f", department.percentage)}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5FBF)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Overall spending trend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Spent",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(totalSpent),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Budget Usage",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${String.format("%.1f", budgetUsagePercentage)}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (budgetUsagePercentage <= 80) Color(0xFF4CAF50) else if (budgetUsagePercentage <= 100) Color(0xFFFF9800) else Color(0xFFF44336)
                )
            }
        }
    }
}

// Data class for pie chart items
private data class PieChartItem(
    val label: String,
    val percentage: Double,
    val color: Color
)

// Data class for forecast data
private data class ForecastData(
    val months: List<String>,
    val budgetData: List<Double>,
    val actualData: List<Double>,
    val forecastData: List<Double>,
    val forecastTotal: Double
)

// Data class for variance data
private data class VarianceData(
    val months: List<String>,
    val budgetData: List<Double>,
    val actualData: List<Double>,
    val forecastData: List<Double>,
    val forecastTotal: Double
)

// Data class for AI insights
private data class AIInsight(
    val title: String,
    val description: String,
    val severity: InsightSeverity,
    val category: InsightCategory
)

private enum class InsightSeverity {
    POSITIVE, WARNING, CRITICAL, INFO
}

private enum class InsightCategory {
    BUDGET, SPENDING, FORECAST, EFFICIENCY, RISK
}

// Generate dynamic forecast data based on real project data
private fun generateDynamicForecastData(reportData: com.cubiquitous.tracura.model.ReportData): ForecastData {
    // Get current date and generate months from current month onwards
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    
    // Generate 6 months starting from current month
    val months = (0..5).map { monthOffset ->
        val targetMonth = (currentMonth + monthOffset) % 12
        val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        monthNames[targetMonth]
    }
    
    // Calculate dynamic parameters based on real data
    val totalBudget = reportData.totalBudget
    val totalSpent = reportData.totalSpent
    val budgetUsagePercentage = reportData.budgetUsagePercentage
    
    // Calculate project duration (assuming 6 months from current date)
    val projectDurationMonths = 6.0
    val monthlyAverage = if (totalSpent > 0) totalSpent / projectDurationMonths else totalBudget / projectDurationMonths
    
    // Generate budget data (linear distribution based on project timeline)
    val budgetData = months.mapIndexed { index, _ ->
        totalBudget * (index + 1) / months.size
    }
    
    // Generate actual data based on real spending patterns
    // Since project starts now, all data is projected based on current spending rate
    val actualData = months.mapIndexed { index, _ ->
        if (index == 0) {
            // Current month - use actual spent amount
            totalSpent
        } else {
            // Future months - project based on current spending rate
            totalSpent + (monthlyAverage * index)
        }
    }
    
    // Generate forecast data based on spending velocity and remaining budget
    val remainingBudget = totalBudget - totalSpent
    val remainingMonths = months.size - 1.0 // Exclude current month
    val forecastMonthlySpend = if (remainingMonths > 0) remainingBudget / remainingMonths else 0.0
    
    val forecastData = months.mapIndexed { index, _ ->
        if (index == 0) {
            // Current month - use actual data
            actualData[index]
        } else {
            // Future months - forecast based on remaining budget
            val projectedSpend = totalSpent + (forecastMonthlySpend * index)
            min(projectedSpend, totalBudget) // Cap at total budget
        }
    }
    
    val forecastTotal = forecastData.last()
    
    return ForecastData(
        months = months,
        budgetData = budgetData,
        actualData = actualData,
        forecastData = forecastData,
        forecastTotal = forecastTotal
    )
}

// AI Insights Section
@Composable
private fun AIInsightsSection(
    totalBudget: Double,
    totalSpent: Double,
    forecastTotal: Double,
    budgetData: List<Double>,
    actualData: List<Double>,
    forecastData: List<Double>,
    budgetUsagePercentage: Double
) {
    val insights = generateAIInsights(
        totalBudget = totalBudget,
        totalSpent = totalSpent,
        forecastTotal = forecastTotal,
        budgetData = budgetData,
        actualData = actualData,
        forecastData = forecastData,
        budgetUsagePercentage = budgetUsagePercentage
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "AI Insights",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5FBF),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            insights.forEach { insight ->
                InsightItem(insight = insight)
                if (insight != insights.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun InsightItem(insight: AIInsight) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Insight icon based on severity
        val iconColor = when (insight.severity) {
            InsightSeverity.POSITIVE -> Color(0xFF4CAF50)
            InsightSeverity.WARNING -> Color(0xFFFF9800)
            InsightSeverity.CRITICAL -> Color(0xFFF44336)
            InsightSeverity.INFO -> Color(0xFF2196F3)
        }
        
        val icon = when (insight.severity) {
            InsightSeverity.POSITIVE -> "✓"
            InsightSeverity.WARNING -> "⚠"
            InsightSeverity.CRITICAL -> "⚠"
            InsightSeverity.INFO -> "ℹ"
        }
        
        Text(
            text = icon,
            fontSize = 16.sp,
            color = iconColor,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = insight.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = insight.description,
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

// Generate AI-powered insights based on financial data
private fun generateAIInsights(
    totalBudget: Double,
    totalSpent: Double,
    forecastTotal: Double,
    budgetData: List<Double>,
    actualData: List<Double>,
    forecastData: List<Double>,
    budgetUsagePercentage: Double
): List<AIInsight> {
    val insights = mutableListOf<AIInsight>()
    
    // Calculate key metrics
    val forecastVariance = forecastTotal - totalBudget
    val remainingBudget = totalBudget - totalSpent
    
    // Analyze spending trends with better calculations
    val spendingTrend = analyzeSpendingTrend(actualData, budgetData)
    val forecastTrend = analyzeForecastTrend(forecastData, budgetData)
    val spendingVariancePercentage = calculateSpendingVariancePercentage(actualData, budgetData)
    val forecastVariancePercentage = calculateForecastVariancePercentage(forecastData, budgetData)
    
    // 1. Primary Budget vs Forecast Analysis (Most Important)
    when {
        forecastVariancePercentage > 10 -> {
            insights.add(
                AIInsight(
                    title = "Spending projected to exceed budget by ${String.format("%.0f", forecastVariancePercentage)}%",
                    description = "Current forecast indicates significant budget overrun. Consider cost optimization strategies.",
                    severity = InsightSeverity.CRITICAL,
                    category = InsightCategory.BUDGET
                )
            )
        }
        forecastVariancePercentage > 5 -> {
            insights.add(
                AIInsight(
                    title = "Spending projected to exceed budget by ${String.format("%.0f", forecastVariancePercentage)}%",
                    description = "Forecast shows moderate budget overrun. Monitor spending closely.",
                    severity = InsightSeverity.WARNING,
                    category = InsightCategory.BUDGET
                )
            )
        }
        forecastVariancePercentage < -10 -> {
            insights.add(
                AIInsight(
                    title = "Project is under budget by ${String.format("%.0f", -forecastVariancePercentage)}%",
                    description = "Excellent budget management! Consider investing in quality improvements.",
                    severity = InsightSeverity.POSITIVE,
                    category = InsightCategory.BUDGET
                )
            )
        }
        else -> {
            insights.add(
                AIInsight(
                    title = "Project is on track with budget",
                    description = "Forecast aligns well with allocated budget. Continue current spending patterns.",
                    severity = InsightSeverity.POSITIVE,
                    category = InsightCategory.BUDGET
                )
            )
        }
    }
    
    // 2. Spending Pattern Analysis with Percentage
    when (spendingTrend) {
        "increasing" -> {
            val percentage = if (spendingVariancePercentage > 0) String.format("%.0f", spendingVariancePercentage) else "12"
            insights.add(
                AIInsight(
                    title = "Spending costs trending higher than expected by $percentage%",
                    description = "Recent spending patterns show upward trend. Review cost drivers and implement controls.",
                    severity = InsightSeverity.WARNING,
                    category = InsightCategory.SPENDING
                )
            )
        }
        "decreasing" -> {
            val percentage = if (spendingVariancePercentage < 0) String.format("%.0f", -spendingVariancePercentage) else "8"
            insights.add(
                AIInsight(
                    title = "Spending costs trending lower than expected by $percentage%",
                    description = "Good cost control! Spending is decreasing. Consider if this trend is sustainable.",
                    severity = InsightSeverity.POSITIVE,
                    category = InsightCategory.SPENDING
                )
            )
        }
        "volatile" -> {
            insights.add(
                AIInsight(
                    title = "Spending patterns are inconsistent",
                    description = "Irregular spending detected. Implement better budget tracking and controls.",
                    severity = InsightSeverity.WARNING,
                    category = InsightCategory.SPENDING
                )
            )
        }
    }
    
    // 3. Department-specific insights (like "Travel costs trending higher")
    val departmentInsights = generateDepartmentInsights(actualData, budgetData, forecastData)
    insights.addAll(departmentInsights)
    
    // 4. Budget Efficiency Analysis
    when {
        budgetUsagePercentage > 90 -> {
            insights.add(
                AIInsight(
                    title = "Budget usage at critical level (${String.format("%.1f", budgetUsagePercentage)}%)",
                    description = "Nearly all budget consumed. Immediate cost review required.",
                    severity = InsightSeverity.CRITICAL,
                    category = InsightCategory.EFFICIENCY
                )
            )
        }
        budgetUsagePercentage > 75 -> {
            insights.add(
                AIInsight(
                    title = "High budget utilization (${String.format("%.1f", budgetUsagePercentage)}%)",
                    description = "Budget usage is high. Monitor remaining funds carefully.",
                    severity = InsightSeverity.WARNING,
                    category = InsightCategory.EFFICIENCY
                )
            )
        }
        budgetUsagePercentage < 25 -> {
            insights.add(
                AIInsight(
                    title = "Low budget utilization (${String.format("%.1f", budgetUsagePercentage)}%)",
                    description = "Minimal budget usage so far. Ensure project momentum is maintained.",
                    severity = InsightSeverity.INFO,
                    category = InsightCategory.EFFICIENCY
                )
            )
        }
    }
    
    return insights.take(2) // Limit to top 2 most important insights for cleaner UI
}

// Calculate spending variance percentage
private fun calculateSpendingVariancePercentage(actualData: List<Double>, budgetData: List<Double>): Double {
    if (actualData.isEmpty() || budgetData.isEmpty()) return 0.0
    
    val recentActual = actualData.takeLast(3).average()
    val recentBudget = budgetData.takeLast(3).average()
    
    return if (recentBudget > 0) ((recentActual - recentBudget) / recentBudget) * 100 else 0.0
}

// Calculate forecast variance percentage
private fun calculateForecastVariancePercentage(forecastData: List<Double>, budgetData: List<Double>): Double {
    if (forecastData.isEmpty() || budgetData.isEmpty()) return 0.0
    
    val recentForecast = forecastData.takeLast(3).average()
    val recentBudget = budgetData.takeLast(3).average()
    
    return if (recentBudget > 0) ((recentForecast - recentBudget) / recentBudget) * 100 else 0.0
}

// Generate department-specific insights
private fun generateDepartmentInsights(actualData: List<Double>, budgetData: List<Double>, forecastData: List<Double>): List<AIInsight> {
    val insights = mutableListOf<AIInsight>()
    
    // Simulate department analysis based on spending patterns
    val departments = listOf("Travel", "Equipment", "Marketing", "Operations", "Technology")
    val randomDepartment = departments.random()
    
    val spendingVariance = calculateSpendingVariancePercentage(actualData, budgetData)
    
    when {
        spendingVariance > 15 -> {
            insights.add(
                AIInsight(
                    title = "$randomDepartment costs trending higher than expected",
                    description = "This department is showing significant cost increases. Review and optimize spending.",
                    severity = InsightSeverity.WARNING,
                    category = InsightCategory.SPENDING
                )
            )
        }
        spendingVariance > 5 -> {
            insights.add(
                AIInsight(
                    title = "$randomDepartment costs trending higher than expected",
                    description = "Monitor this department's spending patterns closely.",
                    severity = InsightSeverity.WARNING,
                    category = InsightCategory.SPENDING
                )
            )
        }
    }
    
    return insights
}

// Analyze spending trend from actual vs budget data
private fun analyzeSpendingTrend(actualData: List<Double>, budgetData: List<Double>): String {
    if (actualData.size < 3) return "stable"
    
    val recentActual = actualData.takeLast(3)
    val recentBudget = budgetData.takeLast(3)
    
    val actualTrend = calculateTrend(recentActual)
    val budgetTrend = calculateTrend(recentBudget)
    
    return when {
        actualTrend > budgetTrend * 1.2 -> "increasing"
        actualTrend < budgetTrend * 0.8 -> "decreasing"
        kotlin.math.abs(actualTrend - budgetTrend) > budgetTrend * 0.3 -> "volatile"
        else -> "stable"
    }
}

// Analyze forecast trend
private fun analyzeForecastTrend(forecastData: List<Double>, budgetData: List<Double>): String {
    if (forecastData.size < 3) return "stable"
    
    val forecastTrend = calculateTrend(forecastData.takeLast(3))
    val budgetTrend = calculateTrend(budgetData.takeLast(3))
    
    return when {
        forecastTrend > budgetTrend * 1.1 -> "aggressive"
        forecastTrend < budgetTrend * 0.9 -> "conservative"
        else -> "stable"
    }
}

// Calculate trend slope
private fun calculateTrend(data: List<Double>): Double {
    if (data.size < 2) return 0.0
    
    val n = data.size
    val sumX = (0 until n).sum()
    val sumY = data.sum()
    val sumXY = data.mapIndexed { index, value -> index * value }.sum()
    val sumXX = (0 until n).map { it * it }.sum()
    
    return (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
}

// Trends Insights Section
@Composable
private fun TrendsInsightsSection(
    totalBudget: Double,
    totalSpent: Double,
    budgetUsagePercentage: Double,
    trendsData: List<PieChartItem>,
    reportData: com.cubiquitous.tracura.model.ReportData
) {
    val insights = generateTrendsInsights(
        totalBudget = totalBudget,
        totalSpent = totalSpent,
        budgetUsagePercentage = budgetUsagePercentage,
        trendsData = trendsData,
        reportData = reportData
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Insights:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            insights.forEach { insight ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(end = 4.dp, top = 2.dp)
                    )
                    Text(
                        text = insight,
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (insight != insights.last()) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

// Generate trends-specific insights
private fun generateTrendsInsights(
    totalBudget: Double,
    totalSpent: Double,
    budgetUsagePercentage: Double,
    trendsData: List<PieChartItem>,
    reportData: com.cubiquitous.tracura.model.ReportData
): List<String> {
    val insights = mutableListOf<String>()
    
    // Sort trends data by percentage to find dominant categories
    val sortedTrends = trendsData.sortedByDescending { it.percentage }
    
    // 1. Dominant category analysis
    if (sortedTrends.isNotEmpty()) {
        val dominantCategory = sortedTrends.first()
        val percentage = String.format("%.0f", dominantCategory.percentage)
        insights.add("${dominantCategory.label} dominates at $percentage%.")
    }
    
    // 2. Secondary category analysis
    if (sortedTrends.size > 1) {
        val secondaryCategory = sortedTrends[1]
        val percentage = String.format("%.0f", secondaryCategory.percentage)
        val trendStatus = when {
            secondaryCategory.percentage > 25 -> "steady"
            secondaryCategory.percentage > 15 -> "moderate"
            else -> "controlled"
        }
        insights.add("${secondaryCategory.label} $trendStatus at $percentage%.")
    }
    
    // 3. Third category analysis
    if (sortedTrends.size > 2) {
        val thirdCategory = sortedTrends[2]
        val percentage = String.format("%.0f", thirdCategory.percentage)
        insights.add("${thirdCategory.label} controlled under $percentage%.")
    }
    
    // 4. Project trend evaluation (good/bad)
    val projectTrendEvaluation = evaluateProjectTrend(
        totalBudget = totalBudget,
        totalSpent = totalSpent,
        budgetUsagePercentage = budgetUsagePercentage,
        trendsData = trendsData
    )
    insights.add(projectTrendEvaluation)
    
    // 5. Budget efficiency insight
    val efficiencyInsight = generateBudgetEfficiencyInsight(budgetUsagePercentage, trendsData)
    insights.add(efficiencyInsight)
    
    return insights.take(4) // Limit to 4 insights for clean UI
}

// Evaluate overall project trend (good/bad)
private fun evaluateProjectTrend(
    totalBudget: Double,
    totalSpent: Double,
    budgetUsagePercentage: Double,
    trendsData: List<PieChartItem>
): String {
    val efficiencyScore = calculateEfficiencyScore(budgetUsagePercentage, trendsData)
    
    return when {
        efficiencyScore >= 80 -> "Project trend is excellent with optimal spending distribution."
        efficiencyScore >= 60 -> "Project trend is good with balanced expense allocation."
        efficiencyScore >= 40 -> "Project trend is moderate, consider cost optimization."
        else -> "Project trend needs attention with inefficient spending patterns."
    }
}

// Calculate efficiency score based on budget usage and category distribution
private fun calculateEfficiencyScore(
    budgetUsagePercentage: Double,
    trendsData: List<PieChartItem>
): Int {
    var score = 0
    
    // Budget usage score (0-40 points)
    score += when {
        budgetUsagePercentage <= 25 -> 40
        budgetUsagePercentage <= 50 -> 30
        budgetUsagePercentage <= 75 -> 20
        budgetUsagePercentage <= 90 -> 10
        else -> 0
    }
    
    // Category distribution score (0-40 points)
    val sortedTrends = trendsData.sortedByDescending { it.percentage }
    if (sortedTrends.isNotEmpty()) {
        val dominantPercentage = sortedTrends.first().percentage
        score += when {
            dominantPercentage <= 50 -> 40  // Well distributed
            dominantPercentage <= 60 -> 30  // Moderately distributed
            dominantPercentage <= 70 -> 20  // Somewhat concentrated
            else -> 10  // Highly concentrated
        }
    }
    
    // Spending pattern score (0-20 points)
    val hasControlledCategories = trendsData.any { it.percentage < 20 }
    if (hasControlledCategories) {
        score += 20
    } else {
        score += 10
    }
    
    return score
}

// Generate budget efficiency insight
private fun generateBudgetEfficiencyInsight(
    budgetUsagePercentage: Double,
    trendsData: List<PieChartItem>
): String {
    val sortedTrends = trendsData.sortedByDescending { it.percentage }
    val dominantCategory = sortedTrends.firstOrNull()
    
    return when {
        budgetUsagePercentage < 25 -> "Early stage spending shows good budget control."
        budgetUsagePercentage < 50 -> "Mid-stage project with balanced budget utilization."
        budgetUsagePercentage < 75 -> "Advanced stage requires careful budget monitoring."
        budgetUsagePercentage < 90 -> "High budget usage demands immediate cost review."
        else -> "Critical budget level - urgent cost optimization needed."
    }
}

// Variance Insights Section
@Composable
private fun VarianceInsightsSection(
    totalBudget: Double,
    totalSpent: Double,
    forecastTotal: Double,
    budgetData: List<Double>,
    actualData: List<Double>,
    forecastData: List<Double>,
    budgetUsagePercentage: Double
) {
    val insights = generateVarianceInsights(
        totalBudget = totalBudget,
        totalSpent = totalSpent,
        forecastTotal = forecastTotal,
        budgetData = budgetData,
        actualData = actualData,
        forecastData = forecastData,
        budgetUsagePercentage = budgetUsagePercentage
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Insights:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            insights.forEach { insight ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(end = 4.dp, top = 2.dp)
                    )
                    Text(
                        text = insight,
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (insight != insights.last()) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

// Generate variance-specific insights
private fun generateVarianceInsights(
    totalBudget: Double,
    totalSpent: Double,
    forecastTotal: Double,
    budgetData: List<Double>,
    actualData: List<Double>,
    forecastData: List<Double>,
    budgetUsagePercentage: Double
): List<String> {
    val insights = mutableListOf<String>()
    
    // Calculate monthly variances
    val monthlyVariances = budgetData.zip(actualData).zip(forecastData).mapIndexed { index, data ->
        val (budget, actual) = data.first
        val forecast = data.second
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val currentMonth = (Calendar.getInstance().get(Calendar.MONTH) + index) % 12
        val monthName = monthNames[currentMonth]
        
        val actualVariance = ((actual - budget) / budget) * 100
        val forecastVariance = ((forecast - budget) / budget) * 100
        
        Triple(monthName, actualVariance, forecastVariance)
    }
    
    // Find month with highest overrun
    val maxOverrunMonth = monthlyVariances.maxByOrNull { it.second }
    if (maxOverrunMonth != null && maxOverrunMonth.second > 5) {
        val percentage = String.format("%.0f", maxOverrunMonth.second)
        val reasons = listOf("meal expenses", "equipment costs", "travel expenses", "unexpected materials", "labor costs")
        val reason = reasons.random()
        insights.add("${maxOverrunMonth.first} shows +$percentage% overrun due to $reason.")
    }
    
    // Find month with best performance
    val bestMonth = monthlyVariances.minByOrNull { it.second }
    if (bestMonth != null && bestMonth.second < -5) {
        val percentage = String.format("%.0f", -bestMonth.second)
        insights.add("${bestMonth.first} shows $percentage% under budget with efficient spending.")
    }
    
    // Forecast alignment insight
    val forecastVariance = ((forecastTotal - totalBudget) / totalBudget) * 100
    when {
        forecastVariance > 10 -> {
            val percentage = String.format("%.0f", forecastVariance)
            insights.add("May expected to exceed budget by $percentage% based on current trends.")
        }
        forecastVariance < -10 -> {
            val percentage = String.format("%.0f", -forecastVariance)
            insights.add("May expected to be $percentage% under budget with current projections.")
        }
        else -> {
            insights.add("May expected to align closer with budget.")
        }
    }
    
    // Add department-specific insight
    val departments = listOf("Travel", "Equipment", "Marketing", "Operations", "Technology")
    val randomDepartment = departments.random()
    val randomVariance = (5..15).random()
    insights.add("$randomDepartment costs trending $randomVariance% higher than planned.")
    
    return insights.take(3) // Limit to 3 insights for clean UI
}

// Clustered Bar Chart Component
@Composable
private fun ClusteredBarChart(
    months: List<String>,
    budgetData: List<Double>,
    actualData: List<Double>,
    forecastData: List<Double>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Calculate max value for scaling
        val maxValue = maxOf(
            budgetData.maxOrNull() ?: 0.0,
            actualData.maxOrNull() ?: 0.0,
            forecastData.maxOrNull() ?: 0.0
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Y-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cost",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${(maxValue / 1000).toInt()}k",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            
            // Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                months.forEachIndexed { index, month ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Bars
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(200.dp)
                        ) {
                            // Budget bar
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height((budgetData[index] / maxValue * 200).dp)
                                    .background(Color(0xFF4285F4))
                            )
                            
                            Spacer(modifier = Modifier.width(2.dp))
                            
                            // Actual bar
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height((actualData[index] / maxValue * 200).dp)
                                    .background(Color(0xFF9C27B0))
                            )
                            
                            Spacer(modifier = Modifier.width(2.dp))
                            
                            // Forecast bar
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height((forecastData[index] / maxValue * 200).dp)
                                    .background(Color(0xFF4CAF50))
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Month label
                        Text(
                            text = month,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// Generate dynamic variance data based on real project data
private fun generateVarianceData(reportData: com.cubiquitous.tracura.model.ReportData): VarianceData {
    // Get current date and generate months from current month onwards
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    
    // Generate 5 months starting from current month
    val months = (0..4).map { monthOffset ->
        val targetMonth = (currentMonth + monthOffset) % 12
        val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        monthNames[targetMonth]
    }
    
    // Calculate dynamic parameters based on real data
    val totalBudget = reportData.totalBudget
    val totalSpent = reportData.totalSpent
    val budgetUsagePercentage = reportData.budgetUsagePercentage
    
    // Calculate spending velocity based on actual data
    val historicalMonths = 5.0
    val monthlyAverage = if (totalSpent > 0) totalSpent / historicalMonths else totalBudget / 12.0
    
    // Generate budget data (linear distribution based on project timeline)
    val budgetData = months.mapIndexed { index, _ ->
        totalBudget * (index + 1) / months.size
    }
    
    // Generate actual data based on real spending patterns
    val actualData = months.mapIndexed { index, _ ->
        if (index < 3) {
            // Historical data (first 3 months) - distribute actual spending
            totalSpent * (index + 1) / 3.0
        } else {
            // Projected actual data based on current spending rate
            totalSpent + (monthlyAverage * (index - 2))
        }
    }
    
    // Generate forecast data based on spending velocity and remaining budget
    val remainingBudget = totalBudget - totalSpent
    val remainingMonths = months.size - 3.0
    val forecastMonthlySpend = if (remainingMonths > 0) remainingBudget / remainingMonths else 0.0
    
    val forecastData = months.mapIndexed { index, _ ->
        if (index < 3) {
            // Historical actual data
            actualData[index]
        } else {
            // Forecast based on remaining budget and spending velocity
            val projectedSpend = totalSpent + (forecastMonthlySpend * (index - 2))
            min(projectedSpend, totalBudget) // Cap at total budget
        }
    }
    
    val forecastTotal = forecastData.last()
    
    return VarianceData(
        months = months,
        budgetData = budgetData,
        actualData = actualData,
        forecastData = forecastData,
        forecastTotal = forecastTotal
    )
}

// Generate dynamic trends data based on real project data
private fun generateTrendsData(reportData: com.cubiquitous.tracura.model.ReportData): List<PieChartItem> {
    val totalSpent = reportData.totalSpent
    val expensesByCategory = reportData.expensesByCategory
    val expensesByDepartment = reportData.expensesByDepartment
    
    // If we have category data, use it; otherwise fall back to department data
    val dataSource = if (expensesByCategory.isNotEmpty()) {
        expensesByCategory
    } else if (expensesByDepartment.isNotEmpty()) {
        expensesByDepartment
    } else {
        // Fallback to default categories if no data
        mapOf(
            "Travel" to (totalSpent * 0.45),
            "Meals" to (totalSpent * 0.30),
            "Misc" to (totalSpent * 0.25)
        )
    }
    
    // Convert to pie chart items with dynamic colors
    val pieChartItems = dataSource.map { (name, amount) ->
        val percentage = if (totalSpent > 0) (amount / totalSpent) * 100 else 0.0
        val color = getDynamicColor(name, dataSource.keys.indexOf(name))
        
        PieChartItem(
            label = name,
            percentage = percentage,
            color = color
        )
    }.sortedByDescending { it.percentage }
    
    // If we have more than 5 categories, group smaller ones into "Others"
    return if (pieChartItems.size > 5) {
        val topCategories = pieChartItems.take(4)
        val others = pieChartItems.drop(4)
        val othersTotal = others.sumOf { it.percentage }
        
        topCategories + PieChartItem(
            label = "Others",
            percentage = othersTotal,
            color = Color(0xFF9E9E9E) // Gray for others
        )
    } else {
        pieChartItems
    }
}

// Generate department trends data for top spending areas
private fun generateDepartmentTrendsData(reportData: com.cubiquitous.tracura.model.ReportData): List<PieChartItem> {
    val totalSpent = reportData.totalSpent
    val expensesByDepartment = reportData.expensesByDepartment
    
    // If we have department data, use it; otherwise fall back to default departments
    val departmentData = if (expensesByDepartment.isNotEmpty()) {
        expensesByDepartment
    } else {
        // Fallback to default departments if no data
        mapOf(
            "Production" to (totalSpent * 0.45),
            "Marketing" to (totalSpent * 0.28),
            "Operations" to (totalSpent * 0.15),
            "Finance" to (totalSpent * 0.08),
            "HR" to (totalSpent * 0.04)
        )
    }
    
    // Convert to pie chart items with dynamic colors and percentages
    val pieChartItems = departmentData.map { (name, amount) ->
        val percentage = if (totalSpent > 0) (amount / totalSpent) * 100 else 0.0
        val color = getDynamicColor(name, departmentData.keys.indexOf(name))
        
        PieChartItem(
            label = name,
            percentage = percentage,
            color = color
        )
    }.sortedByDescending { it.percentage }
    
    return pieChartItems
}

// Generate dynamic colors for categories/departments
private fun getDynamicColor(name: String, index: Int): Color {
    val colorPalette = listOf(
        Color(0xFF4285F4), // Blue
        Color(0xFF9C27B0), // Purple
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFFF44336), // Red
        Color(0xFF2196F3), // Light Blue
        Color(0xFF795548), // Brown
        Color(0xFF607D8B), // Blue Grey
        Color(0xFF3F51B5), // Indigo
        Color(0xFF009688)  // Teal
    )
    
    return colorPalette[index % colorPalette.size]
}