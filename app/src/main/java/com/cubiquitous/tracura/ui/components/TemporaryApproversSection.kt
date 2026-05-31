package com.cubiquitous.tracura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.TemporaryApprover
import com.cubiquitous.tracura.viewmodel.TemporaryApproverViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TemporaryApproversSection(
    projectId: String,
    currentUserId: String,
    viewModel: TemporaryApproverViewModel = hiltViewModel()
) {
    val temporaryApprovers by viewModel.temporaryApprovers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    var showExtendDialog by remember { mutableStateOf(false) }
    var selectedApproverForExtend by remember { mutableStateOf<TemporaryApprover?>(null) }
    
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    // Load temporary approvers when component is first displayed
    LaunchedEffect(projectId) {

//        viewModel.loadTemporaryApprovers(projectId)
        // Also check for expired approvers automatically
        viewModel.checkAndDeactivateExpiredApprovers(projectId)
    }
    
    // Clear success message after showing
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearSuccessMessage()
        }
    }
    
    // Only show section if there are temporary approvers or if loading
    if (temporaryApprovers.isNotEmpty() || isLoading) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Section header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Temporary Approvers",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                // Refresh button
                IconButton(
                    onClick = {
                        viewModel.loadTemporaryApprovers(projectId)
                        viewModel.checkAndDeactivateExpiredApprovers(projectId)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Success message
            successMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                ) {
                    Text(
                        text = message,
                        color = Color(0xFF2E7D32),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            // Error message
            error?.let { errorMsg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = errorMsg,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            // Loading indicator
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF4285F4),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Loading temporary approvers...",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
            
            // Temporary approvers list
            if (temporaryApprovers.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(temporaryApprovers) { tempApprover ->
                        TemporaryApproverItem(
                            tempApprover = tempApprover,
                            dateFormatter = dateFormatter,
                            onDeactivate = {
                                viewModel.deactivateTemporaryApprover(projectId, tempApprover.id)
                            },
                            onExtend = {
                                selectedApproverForExtend = tempApprover
                                showExtendDialog = true
                            },
                            onDelete = {
                                viewModel.deleteTemporaryApprover(projectId, tempApprover.id)
                            }
                        )
                    }
                }
            } else if (!isLoading) {
                Text(
                    text = "No temporary approvers assigned",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
    
    // Extend date dialog
    if (showExtendDialog && selectedApproverForExtend != null) {
        ExtendApproverDialog(
            tempApprover = selectedApproverForExtend!!,
            onDismiss = {
                showExtendDialog = false
                selectedApproverForExtend = null
            },
            onExtend = { newDate ->
                viewModel.updateExpiringDate(
                    projectId = projectId,
                    tempApproverId = selectedApproverForExtend!!.id,
                    newExpiringDate = newDate
                )
                showExtendDialog = false
                selectedApproverForExtend = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemporaryApproverItem(
    tempApprover: TemporaryApprover,
    dateFormatter: SimpleDateFormat,
    onDeactivate: () -> Unit,
    onExtend: () -> Unit,
    onDelete: () -> Unit
) {
    // Calculate if expired and remaining days
    val now = Date()
    val expiringDate = tempApprover.expiringDate?.toDate()
    val isExpired = expiringDate?.before(now) == true
    val remainingDays = if (expiringDate != null) {
        val diffInMillis = expiringDate.time - now.time
        maxOf(0, diffInMillis / (1000 * 60 * 60 * 24))
    } else 0L
    
    val statusColor = when {
        !tempApprover.isActive -> Color(0xFF757575) // Gray for inactive
        isExpired -> Color(0xFFD32F2F) // Red for expired
        remainingDays <= 1 -> Color(0xFFFF9800) // Orange for expiring soon
        else -> Color(0xFF4CAF50) // Green for active
    }
    
    val statusText = when {
        !tempApprover.isActive -> "Inactive"
        isExpired -> "Expired"
        remainingDays <= 0 -> "Expires today"
        remainingDays == 1L -> "Expires tomorrow"
        else -> "$remainingDays days left"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired || !tempApprover.isActive) 
                Color(0xFFF5F5F5) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = statusColor.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = tempApprover.approverName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Temporary Approver Label - only show for active temporary approvers
                    if (tempApprover.isActive) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "TEMPORARY",
                                color = Color(0xFFD32F2F),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Phone and expiry date
            Text(
                text = "Phone: ${tempApprover.approverPhone}",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            tempApprover.expiringDate?.let { expiryDate ->
                Text(
                    text = "Expires: ${dateFormatter.format(expiryDate.toDate())}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Text(
                text = "Assigned by: ${tempApprover.assignedByName}",
                fontSize = 12.sp,
                color = Color.Gray
            )
            
            // Action buttons
            if (tempApprover.isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Extend button
                    if (!isExpired) {
                        OutlinedButton(
                            onClick = onExtend,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF4285F4)
                            )
                        ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Extend", fontSize = 12.sp)
                        }
                    }
                    
                    // Deactivate button
                    OutlinedButton(
                        onClick = onDeactivate,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF9800)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deactivate", fontSize = 12.sp)
                    }
                    
                    // Delete button
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
