package com.cubiquitous.tracura.ui.view.approver.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.model.PaymentMode
import com.cubiquitous.tracura.ui.view.approver.responsiveFontSize
import com.cubiquitous.tracura.ui.view.approver.responsiveIconSize
import com.cubiquitous.tracura.ui.view.approver.responsiveSpacing
import com.cubiquitous.tracura.ui.view.approver.TruncatedTextWithPopup
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.viewmodel.ExpenseViewModel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Description
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserExpensesBottomSheet(
    user: User,
    projectId: String,
    onDismiss: () -> Unit
) {
    val expenseViewModel: ExpenseViewModel = hiltViewModel()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<ExpenseStatus?>(null) }
    
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    // Collect expenses from viewmodel
    val allExpenses by expenseViewModel.expenses.collectAsState()
    
    // Load user expenses for the project
    LaunchedEffect(user.phone, projectId) {
        isLoading = true
        scope.launch {
            try {
                expenseViewModel.loadUserExpensesForProjectDirect(projectId, user.phone)
            } catch (e: Exception) {
                // Error handled
            }
        }
    }
    
    // Update expenses when allExpenses changes
    LaunchedEffect(allExpenses, user.phone) {
        expenses = allExpenses.filter { it.userId == user.phone || it.userName == user.name }
        isLoading = false
    }
    
    // Calculate summary
    val totalExpenses = expenses.size
    val totalAmount = expenses.sumOf { it.amount }
    val approvedAmount = expenses.filter { it.status == ExpenseStatus.APPROVED }.sumOf { it.amount }
    val pendingAmount = expenses.filter { it.status == ExpenseStatus.PENDING }.sumOf { it.amount }
    val rejectedAmount = expenses.filter { it.status == ExpenseStatus.REJECTED }.sumOf { it.amount }
    
    // Filter expenses
    val filteredExpenses = remember(expenses, searchQuery, selectedStatus) {
        expenses.filter { expense ->
            val statusMatch = selectedStatus == null || expense.status == selectedStatus
            val searchMatch = searchQuery.isEmpty() ||
                expense.description.contains(searchQuery, ignoreCase = true) ||
                expense.amount.toString().contains(searchQuery, ignoreCase = true) ||
                expense.category.contains(searchQuery, ignoreCase = true)
            statusMatch && searchMatch
        }
    }
    
    // Get user initial for avatar
    val userInitial = user.name.take(1).uppercase().ifEmpty { "U" }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF5F5F7),
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = Color(0xFFC6C6C8),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with Done button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Done",
                        fontSize = responsiveFontSize(15.sp, 16.sp, 17.sp, 18.sp),
                        color = Color(0xFF007AFF),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        "Search expenses by amount, description, de...", 
                        fontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(responsiveIconSize(18.dp, 20.dp, 22.dp))
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // User Avatar and Name
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar - Light blue background with dark blue letter
                val configuration = LocalConfiguration.current
                val avatarSize = remember(configuration.screenWidthDp) {
                    when {
                        configuration.screenWidthDp < 360 -> 48.dp
                        configuration.screenWidthDp < 480 -> 52.dp
                        else -> 56.dp
                    }
                }
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitial,
                        fontSize = responsiveFontSize(20.sp, 22.sp, 24.sp, 26.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2),
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(responsiveSpacing()))
                
                // User Name
                TruncatedTextWithPopup(
                    text = user.name.ifEmpty { "Unknown User" },
                    maxLength = 15,
                    fontSize = responsiveFontSize(16.sp, 17.sp, 18.sp, 20.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Summary Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Total Expenses Count
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = totalExpenses.toString(),
                        fontSize = responsiveFontSize(20.sp, 22.sp, 24.sp, 26.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(responsiveSpacing() / 2))
                    Text(
                        text = "Total",
                        fontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp),
                        color = Color(0xFF999999),
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(responsiveSpacing()))
                
                // Total Amount
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = FormatUtils.formatCurrency(totalAmount),
                        fontSize = responsiveFontSize(18.sp, 20.sp, 22.sp, 24.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(responsiveSpacing() / 2))
                    Text(
                        text = "Total Amount",
                        fontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp),
                        color = Color(0xFF999999),
                        maxLines = 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status Breakdown Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Approved Chip
                StatusChip(
                    modifier = Modifier.weight(1f),
                    status = "Approved",
                    amount = approvedAmount,
                    color = Color(0xFF4CAF50),
                    backgroundColor = Color(0xFFE8F5E9),
                    isSelected = selectedStatus == ExpenseStatus.APPROVED,
                    onClick = {
                        selectedStatus = if (selectedStatus == ExpenseStatus.APPROVED) null else ExpenseStatus.APPROVED
                    }
                )
                
                // Pending Chip
                StatusChip(
                    modifier = Modifier.weight(1f),
                    status = "Pending",
                    amount = pendingAmount,
                    color = Color(0xFFFF9800),
                    backgroundColor = Color(0xFFFFF3E0),
                    isSelected = selectedStatus == ExpenseStatus.PENDING,
                    onClick = {
                        selectedStatus = if (selectedStatus == ExpenseStatus.PENDING) null else ExpenseStatus.PENDING
                    }
                )
                
                // Rejected Chip
                StatusChip(
                    modifier = Modifier.weight(1f),
                    status = "Rejected",
                    amount = rejectedAmount,
                    color = Color(0xFFF44336),
                    backgroundColor = Color(0xFFFFEBEE),
                    isSelected = selectedStatus == ExpenseStatus.REJECTED,
                    onClick = {
                        selectedStatus = if (selectedStatus == ExpenseStatus.REJECTED) null else ExpenseStatus.REJECTED
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Expense List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4285F4))
                }
            } else if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses found",
                        fontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp),
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredExpenses) { expense ->
                        UserExpenseCard(expense = expense)
                    }
                }
            }
        }
    }
}

// Status Chip Component
@Composable
private fun StatusChip(
    modifier: Modifier = Modifier,
    status: String,
    amount: Double,
    color: Color,
    backgroundColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = status,
                    fontSize = responsiveFontSize(11.sp, 12.sp, 13.sp, 14.sp),
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(responsiveSpacing() / 2))
            Text(
                text = FormatUtils.formatCurrency(amount),
                fontSize = responsiveFontSize(12.sp, 13.sp, 14.sp, 15.sp),
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// User Expense Card Component
@Composable
private fun UserExpenseCard(expense: Expense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top Row - Status and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Status Chip
                StatusBadge(status = expense.status)
                
                // Amount
                Text(
                    text = FormatUtils.formatCurrency(expense.amount),
                    fontSize = responsiveFontSize(14.sp, 15.sp, 16.sp, 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            TruncatedTextWithPopup(
                text = expense.description.ifEmpty { expense.category },
                maxLength = 15,
                fontSize = responsiveFontSize(13.sp, 14.sp, 15.sp, 16.sp),
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(responsiveSpacing()))
            
            // Department
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsiveSpacing() / 2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(responsiveIconSize(12.dp, 13.dp, 14.dp))
                )
                TruncatedTextWithPopup(
                    text = expense.department.ifEmpty { "No Department" },
                    maxLength = 15,
                    fontSize = responsiveFontSize(11.sp, 12.sp, 13.sp, 14.sp),
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF999999)
                )
            }
            
            Spacer(modifier = Modifier.height(responsiveSpacing() / 2))
            
            // Sub Category
            if (expense.category.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(responsiveSpacing() / 2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(responsiveIconSize(12.dp, 13.dp, 14.dp))
                    )
                    Text(
                        text = "Sub Category: ${expense.category}",
                        fontSize = responsiveFontSize(11.sp, 12.sp, 13.sp, 14.sp),
                        color = Color(0xFF999999),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.height(responsiveSpacing() / 2))
            }
            
            // Submitted By
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsiveSpacing() / 2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(responsiveIconSize(12.dp, 13.dp, 14.dp))
                )
                Text(
                    text = "Submitted by: ${expense.userName.ifEmpty { "Unknown" }}",
                    fontSize = responsiveFontSize(11.sp, 12.sp, 13.sp, 14.sp),
                    color = Color(0xFF999999),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            
            Spacer(modifier = Modifier.height(responsiveSpacing() / 4))
            
            // Date & Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsiveSpacing() / 2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(responsiveIconSize(12.dp, 13.dp, 14.dp))
                )
                Text(
                    text = formatExpenseDate(expense.getDateAsTimestamp()),
                    fontSize = responsiveFontSize(11.sp, 12.sp, 13.sp, 14.sp),
                    color = Color(0xFF999999),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Payment Method with Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icons for payment proof and attachment
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    
                    // Payment Proof Icon (if exists)
                    if (expense.paymentProofUrl.isNotEmpty()) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Payment Proof",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    com.cubiquitous.tracura.utils.ImageUriHelper.openFirebaseStorageFile(
                                        context = context,
                                        storageUrl = expense.paymentProofUrl,
                                        fileName = expense.paymentProofFileName,
                                        onError = { error ->
                                            android.widget.Toast.makeText(
                                                context,
                                                "Error: $error",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                        )
                    }
                    
                    // Attachment Icon (if exists)
                    if (expense.attachmentUrl.isNotEmpty()) {
                        val attachmentIcon = if (expense.attachmentFileName.contains(".jpg", ignoreCase = true) ||
                            expense.attachmentFileName.contains(".jpeg", ignoreCase = true) ||
                            expense.attachmentFileName.contains(".png", ignoreCase = true) ||
                            expense.attachmentFileName.contains("image", ignoreCase = true)) {
                            Icons.Default.Image
                        } else {
                            Icons.Default.Description
                        }
                        
                        val attachmentColor = if (attachmentIcon == Icons.Default.Image) {
                            Color(0xFF4CAF50)
                        } else {
                            Color(0xFF2196F3)
                        }
                        
                        Icon(
                            attachmentIcon,
                            contentDescription = "Attachment",
                            tint = attachmentColor,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    com.cubiquitous.tracura.utils.ImageUriHelper.openFirebaseStorageFile(
                                        context = context,
                                        storageUrl = expense.attachmentUrl,
                                        fileName = expense.attachmentFileName,
                                        onError = { error ->
                                            android.widget.Toast.makeText(
                                                context,
                                                "Error: $error",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                        )
                    }
                }
                
                PaymentMethodChip(modeOfPayment = PaymentMode.toString(expense.modeOfPayment))
            }
        }
    }
}

// Status Badge Component
@Composable
private fun StatusBadge(status: ExpenseStatus) {
    val (text, color, backgroundColor) = when (status) {
        ExpenseStatus.APPROVED -> Triple("Approved", Color(0xFF1976D2), Color(0xFFE3F2FD)) // Blue
        ExpenseStatus.COMPLETE -> Triple("Complete", Color(0xFF4CAF50), Color(0xFFE8F5E9)) // Green
        ExpenseStatus.PENDING -> Triple("Pending", Color(0xFFFF9800), Color(0xFFFFF3E0))
        ExpenseStatus.REJECTED -> Triple("Rejected", Color(0xFFF44336), Color(0xFFFFEBEE))
        else -> Triple("Draft", Color.Gray, Color(0xFFF5F5F5))
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

// Payment Method Chip Component
@Composable
private fun PaymentMethodChip(modeOfPayment: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = when (modeOfPayment.lowercase()) {
                    "cash" -> "By cash"
                    "upi" -> "By UPI"
                    "check" -> "By Cheque"
                    else -> "By ${modeOfPayment}"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1976D2)
            )
        }
    }
}

// Helper function to format expense date
private fun formatExpenseDate(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return "No date"
    val date = timestamp.toDate()
    val dateFormat = java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.US)
    return dateFormat.format(date)
}
