package com.cubiquitous.tracura.ui.view.user

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Badge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseSummary
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.ExpenseViewModel
import com.cubiquitous.tracura.viewmodel.NotificationViewModel
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.model.Notification
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    onNavigateToProjectSelection: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToExpenseList: () -> Unit,
    onNavigateToTrackSubmissions: () -> Unit,
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProject: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val expenseSummary by expenseViewModel.expenseSummary.collectAsState(initial = ExpenseSummary())
    val isLoading by expenseViewModel.isLoading.collectAsState()
    val error by expenseViewModel.error.collectAsState()
    val successMessage by expenseViewModel.successMessage.collectAsState()
    
    // Notification state
    val notifications by notificationViewModel.notifications.collectAsState()
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val isNotificationsLoading by notificationViewModel.isLoading.collectAsState()
    
    // Coroutine scope for logout
    val scope = rememberCoroutineScope()

    // Refresh user data when screen opens to ensure current user is loaded
    LaunchedEffect(Unit) {
        authViewModel.refreshUserData()
    }

    // Load user expenses when screen opens
    LaunchedEffect(authState.user?.uid) {
        authState.user?.uid?.let { userId ->
            expenseViewModel.loadUserExpenses(userId)
        }
    }
    
    // Load notifications when screen opens
    LaunchedEffect(authState.user?.phone) {
        authState.user?.phone?.let { userId ->
            notificationViewModel.loadNotifications(userId)
        }
    }

    // Auto-refresh every 30 seconds to get latest approved expenses
    LaunchedEffect(authState.user?.uid) {
        while (true) {
            kotlinx.coroutines.delay(30000) // 30 seconds
            authState.user?.uid?.let { userId ->
                expenseViewModel.refreshData(userId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TRACURA",
                        fontSize = 30.sp, // Made bigger as requested
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4285F4),
                    titleContentColor = Color.White
                ),
                actions = {
                    // Notification icon with badge
                    Box {
                        IconButton(
                            onClick = { onNavigateToNotifications() }
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                        // Notification badge
                        if (notificationBadge.hasUnread && notificationBadge.count > 0) {
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = if (notificationBadge.count > 99) "99+" else notificationBadge.count.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = {
                            authState.user?.uid?.let { userId ->
                                expenseViewModel.refreshData(userId)
                            }
                            authState.user?.phone?.let { userId ->
                                notificationViewModel.refreshNotifications(userId)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                    
                    // Logout button
                    IconButton(
                        onClick = { 
                            println("🚪 Logout button clicked")
                            scope.launch {
                                authViewModel.performLogout()
                                onLogout()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Welcome Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4285F4))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Welcome back!",
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = authState.user?.name ?: "User",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Track and manage your project expenses",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    
                    // Success Message
                    if (successMessage != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = successMessage!!,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { expenseViewModel.clearSuccessMessage() }
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Error Message
                    if (error != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = error!!,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { expenseViewModel.clearExpenseError() }
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Notifications Section (if there are unread notifications)
                    if (notifications.isNotEmpty() && !isNotificationsLoading) {
                        item {
                            Text(
                                text = "Recent Notifications",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        // Show up to 3 most recent notifications
                        items(notifications.take(3)) { notification ->
                            NotificationCard(
                                notification = notification,
                                onClick = {
                                    if (notification.projectId.isNotEmpty()) {
                                        onNavigateToProject(notification.projectId)
                                    } else {
                                        onNavigateToNotifications()
                                    }
                                }
                            )
                        }
                        
                        // Show "View All" button if there are more notifications
                        if (notifications.size > 3) {
                            item {
                                TextButton(
                                    onClick = { onNavigateToNotifications() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "View All Notifications (${notifications.size})",
                                        color = Color(0xFF4285F4)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Quick Actions
                    item {
                        Text(
                            text = "Quick Actions",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                title = "Add Expense",
                                subtitle = "Submit new",
                                icon = Icons.Default.Add,
                                onClick = onNavigateToAddExpense
                            )
                            
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                title = "View Expenses",
                                subtitle = "See all",
                                icon = Icons.Default.List,
                                onClick = onNavigateToExpenseList
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                title = "Track Status",
                                subtitle = "Check approvals",
                                icon = Icons.Default.DateRange,
                                onClick = onNavigateToTrackSubmissions
                            )
                            
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                title = "Select Project",
                                subtitle = "Change project",
                                icon = Icons.Default.LocationOn,
                                onClick = onNavigateToProjectSelection
                            )
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ActionCard(
                                modifier = Modifier.weight(1f),
                                title = "Team Chat",
                                subtitle = "Select project",
                                icon = Icons.Default.Person,
                                onClick = onNavigateToProjectSelection
                            )
                            
                            // Empty space to balance the row
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    
                    // Update User Names Button (for development/testing)
                    item {
                        Button(
                            onClick = {
                                authState.user?.uid?.let { userId ->
                                    authState.selectedProject?.id?.let { projectId ->
                                        expenseViewModel.updateExistingExpenseUserNames(
                                            projectId = projectId,
                                            userId = userId,
                                            userName = "Deeksha"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9C27B0)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Update Existing Expenses with User Name",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Expense Analytics Section
                    item {
                        Text(
                            text = "Your Expense Analytics",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    // Summary Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ExpenseSummaryCard(
                                modifier = Modifier.weight(1f),
                                title = "Approved",
                                amount = expenseSummary.totalApproved,
                                count = expenseSummary.approvedCount,
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFF4CAF50)
                            )
                            
                            ExpenseSummaryCard(
                                modifier = Modifier.weight(1f),
                                title = "Pending",
                                amount = expenseSummary.totalPending,
                                count = expenseSummary.pendingCount,
                                icon = Icons.Default.Warning,
                                color = Color(0xFFFF9800)
                            )
                            
                            ExpenseSummaryCard(
                                modifier = Modifier.weight(1f),
                                title = "Rejected",
                                amount = expenseSummary.totalRejected,
                                count = expenseSummary.rejectedCount,
                                icon = Icons.Default.Close,
                                color = Color(0xFFF44336)
                            )
                        }
                    }

                    // Budget Allocation Pie Chart - Only show if there are approved expenses
                    if (expenseSummary.expensesByCategory.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Approved Expenses by Category",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Pie Chart
                                        PieChart(
                                            data = expenseSummary.expensesByCategory,
                                            modifier = Modifier.size(120.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        // Legend
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            expenseSummary.expensesByCategory.entries.forEachIndexed { index, (category, amount) ->
                                                ChartLegendItem(
                                                    color = getChartColor(index),
                                                    category = category,
                                                    amount = amount
                                                )
                                                if (index < expenseSummary.expensesByCategory.size - 1) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Project-wise Breakdown - Only show if there are approved expenses
                    if (expenseSummary.expensesByProject.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Approved Expenses by Project",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    expenseSummary.expensesByProject.entries.forEach { (project, amount) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = project,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.Black,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = FormatUtils.formatCurrency(amount),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Recent Approved Expenses - Only show if there are any
                    if (expenseSummary.recentExpenses.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Recent Approved Expenses",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }

                    // Recent Approved Expense Items
                    items(expenseSummary.recentExpenses) { expense ->
                        RecentExpenseItem(expense = expense)
                    }

                    // Show motivational message if no approved expenses yet
                    if (expenseSummary.approvedCount == 0 && expenseSummary.pendingCount == 0) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color(0xFF4285F4)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Start Your Expense Journey!",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4285F4),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Submit your first expense to see analytics and track your spending",
                                        fontSize = 14.sp,
                                        color = Color(0xFF4285F4),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = onNavigateToAddExpense,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4285F4)
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Add First Expense")
                                    }
                                }
                            }
                        }
                    }

                    // Error handling
                    error?.let {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = it,
                                    color = Color.Red,
                                    modifier = Modifier.padding(16.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickableCardAnimation()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF4285F4)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ExpenseSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    count: Int,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = FormatUtils.formatCurrency(amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "$count item${if (count != 1) "s" else ""}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun PieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    
    Canvas(modifier = modifier) {
        if (total > 0) {
            var startAngle = -90f
            data.entries.forEachIndexed { index, (_, value) ->
                val sweepAngle = (value / total * 360).toFloat()
                drawArc(
                    color = getChartColor(index),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun ChartLegendItem(
    color: Color,
    category: String,
    amount: Double
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
        Column {
            Text(
                text = category,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = FormatUtils.formatCurrency(amount),
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RecentExpenseItem(expense: Expense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Approved",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = expense.department,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                expense.reviewedAt?.let {
                    Text(
                        text = "Approved: ${FormatUtils.formatDate(it)}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Text(
                text = FormatUtils.formatCurrency(expense.amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

fun getChartColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF4285F4), // Blue
        Color(0xFF34A853), // Green
        Color(0xFFEA4335), // Red
        Color(0xFFFBBC05), // Yellow
        Color(0xFF9C27B0), // Purple
        Color(0xFFFF9800), // Orange
        Color(0xFF795548), // Brown
        Color(0xFF607D8B)  // Blue Grey
    )
    return colors[index % colors.size]
}

// Extension function for clickable card animation
@Composable
fun Modifier.clickableCardAnimation(): Modifier {
    return this.clickable { }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) Color(0xFFE3F2FD) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.isRead) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Notification icon
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = if (!notification.isRead) Color(0xFF4285F4) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Notification content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    fontSize = 16.sp,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (notification.projectName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Project: ${notification.projectName}",
                        fontSize = 12.sp,
                        color = Color(0xFF4285F4),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Unread indicator
            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF4285F4), CircleShape)
                )
            }
        }
    }
} 