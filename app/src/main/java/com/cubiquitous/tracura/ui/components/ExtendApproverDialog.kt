package com.cubiquitous.tracura.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cubiquitous.tracura.model.TemporaryApprover
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtendApproverDialog(
    tempApprover: TemporaryApprover,
    onDismiss: () -> Unit,
    onExtend: (Date) -> Unit
) {
    var newExpiringDate by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState()
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    // Handle date selection safely
    LaunchedEffect(datePickerState.selectedDateMillis) {
        try {
            datePickerState.selectedDateMillis?.let { millis ->
                if (millis > 0) {
                    newExpiringDate = Date(millis)
                    showDatePicker = false
                }
            }
        } catch (e: Exception) {
            // Handle date parsing errors silently
            showDatePicker = false
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Extend Approval Period",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Current approver info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Approver: ${tempApprover.approverName}",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Phone: ${tempApprover.approverPhone}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        tempApprover.expiringDate?.let { currentExpiry ->
                            Text(
                                text = "Current expiry: ${dateFormatter.format(currentExpiry.toDate())}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // New expiring date selection
                Text(
                    text = "New Expiration Date:",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = newExpiringDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Select new expiration date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select Date",
                                tint = Color(0xFF4285F4)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4285F4),
                        focusedLabelColor = Color(0xFF4285F4)
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            newExpiringDate?.let { date ->
                                onExtend(date)
                            }
                        },
                        enabled = newExpiringDate != null,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4)
                        )
                    ) {
                        Text("Extend")
                    }
                }
            }
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            datePickerState.selectedDateMillis?.let { millis ->
                                if (millis > 0) {
                                    newExpiringDate = Date(millis)
                                }
                            }
                        } catch (e: Exception) {
                            // Handle date parsing errors silently
                        } finally {
                            showDatePicker = false
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
