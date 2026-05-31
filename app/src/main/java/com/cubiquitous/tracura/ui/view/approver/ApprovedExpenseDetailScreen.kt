package com.cubiquitous.tracura.ui.view.approver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.ExpenseStatus
import com.cubiquitous.tracura.model.PaymentMode
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.viewmodel.ApprovalViewModel

private data class ApprovedExpenseDetailColors(
    val background: Color,
    val card: Color,
    val field: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentSoft: Color,
    val success: Color,
    val successSoft: Color,
    val error: Color,
    val errorSoft: Color
)

@Composable
private fun rememberApprovedExpenseDetailColors(): ApprovedExpenseDetailColors {
    val isDark = isSystemInDarkTheme()
    return ApprovedExpenseDetailColors(
        background = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
        card = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
        field = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
        textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
        textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43),
        accent = MaterialTheme.colorScheme.primary,
        accentSoft = if (isDark) Color(0xFF2C2C2E) else Color(0xFFEAF2FF),
        success = if (isDark) Color(0xFF34C759) else Color(0xFF2E7D32),
        successSoft = if (isDark) Color(0xFF1F2B22) else Color(0xFFE8F5E9),
        error = if (isDark) Color(0xFFFF6B6B) else Color(0xFFC62828),
        errorSoft = if (isDark) Color(0xFF3A1F22) else Color(0xFFFFEBEE)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovedExpenseDetailScreen(
    expenseId: String,
    onNavigateBack: () -> Unit,
    approvalViewModel: ApprovalViewModel = hiltViewModel()
) {
    val selectedExpense by approvalViewModel.selectedExpense.collectAsState()
    val isLoading by approvalViewModel.isLoading.collectAsState()
    val colors = rememberApprovedExpenseDetailColors()

    LaunchedEffect(expenseId) {
        approvalViewModel.fetchExpenseById(expenseId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Expense Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                },
                actions = {
                    TextButton(onClick = onNavigateBack) {
                        Text(text = "Done", color = colors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.card,
                    titleContentColor = colors.textPrimary,
                    actionIconContentColor = colors.accent
                )
            )
        },
        containerColor = colors.background
    ) { padding ->
        if (isLoading || selectedExpense == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.accent)
            }
            return@Scaffold
        }

        val expense = selectedExpense!!
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount + Status
            Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Amount", color = colors.textSecondary, fontSize = 14.sp)
                        // Use remainingBalance if not null and >= 1, otherwise use amount
                        val displayAmount = if (expense.remainingBalance != null && expense.remainingBalance!! >= 1) {
                            expense.remainingBalance!!
                        } else {
                            expense.amount
                        }
                        Text(
                            text = FormatUtils.formatCurrency(displayAmount),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        expense.phaseName?.let { phase ->
                            Text(
                                text = "Phase: $phase",
                                color = colors.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Department: ${FormatUtils.getDepartmentDisplayName(expense.department)}",
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                        val categoriesText = if (expense.categories.isNotEmpty()) {
                            expense.categories.joinToString(", ")
                        } else {
                            ""
                        }
                        Text(
                            text = "Categories: $categoriesText",
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }

                    // Status badge for approved or rejected expenses
                    when (expense.status) {
                        ExpenseStatus.APPROVED -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(colors.accentSoft, RoundedCornerShape(24.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Approved",
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = expense.status.name.replaceFirstChar { it.titlecase() },
                                    color = colors.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        ExpenseStatus.REJECTED -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(colors.errorSoft, RoundedCornerShape(24.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Rejected",
                                    tint = colors.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = expense.status.name.replaceFirstChar { it.titlecase() },
                                    color = colors.error,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        else -> {
                            // For other statuses (PENDING, DRAFT), show a neutral badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(colors.field, RoundedCornerShape(24.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = expense.status.name.replaceFirstChar { it.titlecase() },
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Expense Details
            Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = "Expense Details", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(label = "Date", value = FormatUtils.formatDate(expense.date, "MMM d, yyyy"), colors = colors)
                    val submittedByVal = expense.submittedBy.ifBlank { expense.userName.ifBlank { expense.userId } }
                    DetailRow(label = "Submitted By", value = submittedByVal, colors = colors)
                    val descriptionVal = expense.description.ifBlank { "—" }
                    DetailRow(label = "Description", value = descriptionVal, colors = colors)
                    val remarkVal = expense.remark?.ifBlank { null } ?: "—"
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Remark:", color = colors.textSecondary)
                        Text(text = remarkVal, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Material Details
            if (hasMaterialDetails(expense)) {
                Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Material Details",
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(text = "Material Details", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        expense.itemType?.let { itemType ->
                            MaterialDetailRow(
                                icon = Icons.Default.Label,
                                iconColor = colors.accent,
                                label = "Sub-category",
                                value = itemType,
                                colors = colors
                            )
                        }
                        
                        expense.item?.let { item ->
                            MaterialDetailRow(
                                icon = Icons.Default.ShoppingCart,
                                iconColor = Color(0xFFFF9800),
                                label = "Material",
                                value = item,
                                colors = colors
                            )
                        }
                        
                        expense.brand?.let { brand ->
                            MaterialDetailRow(
                                icon = Icons.Default.Category,
                                iconColor = Color(0xFF9C27B0),
                                label = "Brand",
                                value = brand,
                                colors = colors
                            )
                        }
                        
                        expense.spec?.let { spec ->
                            MaterialDetailRow(
                                icon = Icons.Default.Star,
                                iconColor = Color(0xFFFFC107),
                                label = "Grade",
                                value = spec,
                                colors = colors
                            )
                        }
                        
                        expense.thickness?.let { thickness ->
                            MaterialDetailRow(
                                icon = Icons.Default.Height,
                                iconColor = Color(0xFF607D8B),
                                label = "Thickness",
                                value = thickness,
                                colors = colors
                            )
                        }
                        
                        expense.quantity?.let { quantity ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                             Icon(
                                                 imageVector = Icons.Default.Pin,
                                                 contentDescription = "Quantity",
                                                 tint = Color(0xFF607D8B),
                                                 modifier = Modifier.size(18.dp)
                                             )
                                            Text(text = "Quantity:", color = colors.textSecondary, fontSize = 14.sp)
                                         }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = quantity, color = colors.textPrimary, fontSize = 14.sp)
                                    expense.uom?.let { uom ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                 Icon(
                                                     imageVector = Icons.Default.Balance,
                                                     contentDescription = "UoM",
                                                     tint = colors.accent,
                                                     modifier = Modifier.size(18.dp)
                                                 )
                                                Text(text = "UoM: $uom", color = colors.textPrimary, fontSize = 14.sp)
                                             }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        expense.unitPrice?.let { unitPrice ->
                            val unitPriceDouble = parseNumber(unitPrice)
                            val quantityDouble = expense.quantity?.let { parseNumber(it) } ?: 0.0
                            val lineAmount = unitPriceDouble * quantityDouble
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = "Unit Price",
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(text = "Unit Price:", color = colors.textSecondary, fontSize = 14.sp)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = FormatUtils.formatCurrency(unitPriceDouble),
                                        color = colors.textPrimary,
                                        fontSize = 14.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AttachMoney,
                                            contentDescription = "Line Amount",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Line Amount: ${FormatUtils.formatCurrency(lineAmount)}",
                                            color = colors.textPrimary,
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

            // Payment Information
            Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = "Payment Information", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    val mode = when (expense.modeOfPayment) {
                        PaymentMode.CASH -> "By cash"
                        PaymentMode.UPI -> "By UPI"
                        PaymentMode.CHEQUE -> "By Cheque"
                        else -> "Not specified"
                    }
                    DetailRow(label = "Payment Mode", value = mode, colors = colors)
                }
            }

            // Approval/Rejection Information
            Card(colors = CardDefaults.cardColors(containerColor = colors.card), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = if (expense.status == ExpenseStatus.APPROVED) "Approval Information" else "Review Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(label = "Last Updated", value = FormatUtils.formatDateTime(expense.reviewedAt), colors = colors)
                    when (expense.status) {
                        ExpenseStatus.APPROVED -> {
                            DetailRow(
                                label = "Approved By",
                                value = expense.approvedBy ?: expense.reviewedBy ?: "—",
                                colors = colors
                            )
                        }
                        ExpenseStatus.REJECTED -> {
                            DetailRow(
                                label = "Rejected By",
                                value = expense.reviewedBy ?: expense.approvedBy ?: "—",
                                colors = colors
                            )
                        }
                        else -> {
                            DetailRow(
                                label = "Reviewed By",
                                value = expense.reviewedBy ?: expense.approvedBy ?: "—",
                                colors = colors
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Status-specific message card
                    when (expense.status) {
                        ExpenseStatus.APPROVED -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.successSoft, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.success,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "This expense has been approved and processed",
                                    color = colors.success,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        ExpenseStatus.REJECTED -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.errorSoft, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = colors.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "This expense has been rejected",
                                    color = colors.error,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        else -> {
                            // For other statuses, show a neutral message
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.field, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "This expense is ${expense.status.name.lowercase()}",
                                    color = colors.textSecondary,
                                    modifier = Modifier.weight(1f)
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
private fun DetailRow(label: String, value: String, colors: ApprovedExpenseDetailColors) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = colors.textSecondary, fontSize = 14.sp)
        Text(text = value, color = colors.textPrimary, fontSize = 14.sp)
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun MaterialDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    colors: ApprovedExpenseDetailColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Text(text = "$label:", color = colors.textSecondary, fontSize = 14.sp)
        }
        Text(text = value, color = colors.textPrimary, fontSize = 14.sp)
    }
    Spacer(modifier = Modifier.height(8.dp))
}

private fun hasMaterialDetails(expense: com.cubiquitous.tracura.model.Expense): Boolean {
    return expense.itemType != null || expense.item != null || expense.brand != null ||
            expense.spec != null || expense.thickness != null || expense.quantity != null ||
            expense.uom != null || expense.unitPrice != null
}

private fun parseNumber(value: String): Double {
    return try {
        // Remove commas and parse
        value.replace(",", "").replace(" ", "").toDouble()
    } catch (e: Exception) {
        0.0
    }
}

private fun defaultApprovalMessage(expense: com.cubiquitous.tracura.model.Expense): String =
    if (expense.status == ExpenseStatus.APPROVED) "This expense has been approved and processed" else "—"

