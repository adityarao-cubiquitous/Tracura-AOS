package com.cubiquitous.tracura.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.AmountFilterRange
import com.cubiquitous.tracura.utils.FormatUtils
import java.text.NumberFormat
import java.util.*

/**
 * Filter Chip Component
 */
@Composable
fun FilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else 
                Color.White
        ),
        border = if (!isSelected) {
            BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
        } else null,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else Color.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * Date Filter Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterSheet(
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Filter by Date",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn {
                item {
                    FilterOption(
                        title = "All Dates",
                        isSelected = selectedFilter == null,
                        onClick = {
                            onFilterSelected(null)
                            onDismiss()
                        }
                    )
                }
                item {
                    FilterOption(
                        title = "Today",
                        isSelected = selectedFilter == "Today",
                        onClick = {
                            onFilterSelected("Today")
                            onDismiss()
                        }
                    )
                }
                item {
                    FilterOption(
                        title = "This Week",
                        isSelected = selectedFilter == "This Week",
                        onClick = {
                            onFilterSelected("This Week")
                            onDismiss()
                        }
                    )
                }
                item {
                    FilterOption(
                        title = "This Month",
                        isSelected = selectedFilter == "This Month",
                        onClick = {
                            onFilterSelected("This Month")
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Department Filter Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentFilterSheet(
    availableDepartments: List<String>,
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Filter by Department",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn {
                item {
                    FilterOption(
                        title = "All Departments",
                        isSelected = selectedFilter == null,
                        onClick = {
                            onFilterSelected(null)
                            onDismiss()
                        }
                    )
                }
                
                items(availableDepartments) { department ->
                    FilterOption(
                        title = FormatUtils.getDepartmentDisplayName(department),
                        isSelected = selectedFilter == department,
                        onClick = {
                            onFilterSelected(department)
                            onDismiss()
                        }
                    )
                }
                
                if (availableDepartments.isEmpty()) {
                    item {
                        Text(
                            "No departments available",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Amount Filter Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountFilterSheet(
    selectedFilter: AmountFilterRange?,
    onFilterSelected: (AmountFilterRange?) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustomRange by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Filter by Amount",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn {
                item {
                    FilterOption(
                        title = "All Amounts",
                        isSelected = selectedFilter == null,
                        onClick = {
                            onFilterSelected(null)
                            onDismiss()
                        }
                    )
                }
                item {
                    FilterOption(
                        title = "Under ₹1,000",
                        isSelected = selectedFilter is AmountFilterRange.Under1000,
                        onClick = {
                            onFilterSelected(AmountFilterRange.Under1000)
                            onDismiss()
                        }
                    )
                }
                item {
                    FilterOption(
                        title = "₹1,000 - ₹5,000",
                        isSelected = selectedFilter is AmountFilterRange.Between1000And5000,
                        onClick = {
                            onFilterSelected(AmountFilterRange.Between1000And5000)
                            onDismiss()
                        }
                    )
                }
                item {
                    FilterOption(
                        title = "₹5,000 - ₹10,000",
                        isSelected = selectedFilter is AmountFilterRange.Between5000And10000,
                        onClick = {
                            onFilterSelected(AmountFilterRange.Between5000And10000)
                            onDismiss()
                        }
                    )
                }
                item {
                    FilterOption(
                        title = "Over ₹10,000",
                        isSelected = selectedFilter is AmountFilterRange.Over10000,
                        onClick = {
                            onFilterSelected(AmountFilterRange.Over10000)
                            onDismiss()
                        }
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCustomRange = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Custom Range")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedFilter is AmountFilterRange.Custom) {
                                Icon(
                                    Icons.Default.Check,
                                    "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowRight, null)
                        }
                    }
                    
                    if (selectedFilter is AmountFilterRange.Custom) {
                        Text(
                            "Current Range: ${formatCurrency(selectedFilter.min.toInt())} - ${formatCurrency(selectedFilter.max.toInt())}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
    
    if (showCustomRange) {
        CustomAmountRangeSheet(
            minAmount = if (selectedFilter is AmountFilterRange.Custom) selectedFilter.min else 0.0,
            maxAmount = if (selectedFilter is AmountFilterRange.Custom) selectedFilter.max else 100000.0,
            onApply = { min, max ->
                onFilterSelected(AmountFilterRange.Custom(min, max))
                showCustomRange = false
            },
            onDismiss = { showCustomRange = false }
        )
    }
}

/**
 * Custom Amount Range Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAmountRangeSheet(
    minAmount: Double,
    maxAmount: Double,
    onApply: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var localMin by remember { mutableStateOf(minAmount.toString()) }
    var localMax by remember { mutableStateOf(maxAmount.toString()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val minValue = localMin.toDoubleOrNull() ?: 0.0
    val maxValue = localMax.toDoubleOrNull() ?: 0.0
    val isValid = minValue < maxValue && minValue >= 0 && maxValue >= 0
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Custom Amount Range",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                "Minimum Amount",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = localMin,
                onValueChange = { localMin = it },
                label = { Text("Minimum Amount") },
                placeholder = { Text("₹0") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Text("₹", modifier = Modifier.padding(start = 8.dp)) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Maximum Amount",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = localMax,
                onValueChange = { localMax = it },
                label = { Text("Maximum Amount") },
                placeholder = { Text("₹100000") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Text("₹", modifier = Modifier.padding(start = 8.dp)) }
            )
            
            if (!isValid && localMin.isNotEmpty() && localMax.isNotEmpty()) {
                Text(
                    "Minimum amount must be less than maximum amount",
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        onApply(minValue, maxValue)
                        onDismiss()
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

/**
 * Status / Type filter options for Business Head Pending Approvals.
 * null = Default: (byCredit == false && status == APPROVED) || (status == PENDING)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusFilterSheet(
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Filter by Status",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    FilterOption(
                        title = "Default (Pending + Approved non-credit)",
                        isSelected = selectedFilter == null,
                        onClick = {
                            onFilterSelected(null)
                            onDismiss()
                        }
                    )
                }
                item {
                    FilterOption(
                        title = "All",
                        isSelected = selectedFilter == "all",
                        onClick = {
                            onFilterSelected("all")
                            onDismiss()
                        }
                    )
                }
                item {
                    FilterOption(
                        title = "Pending only",
                        isSelected = selectedFilter == "pending",
                        onClick = {
                            onFilterSelected("pending")
                            onDismiss()
                        }
                    )
                }
                item {
                    FilterOption(
                        title = "Approved (non-credit only)",
                        isSelected = selectedFilter == "approved_non_credit",
                        onClick = {
                            onFilterSelected("approved_non_credit")
                            onDismiss()
                        }
                    )
                }
                item {
                    FilterOption(
                        title = "Approved (all)",
                        isSelected = selectedFilter == "approved_all",
                        onClick = {
                            onFilterSelected("approved_all")
                            onDismiss()
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Filter Option Row
 */
@Composable
fun FilterOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun formatCurrency(amount: Int): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}
