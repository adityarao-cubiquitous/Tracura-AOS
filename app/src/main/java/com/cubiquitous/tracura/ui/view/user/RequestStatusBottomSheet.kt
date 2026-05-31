package com.cubiquitous.tracura.ui.view.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.Phase
import com.cubiquitous.tracura.viewmodel.ExpenseViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestStatusBottomSheet(
    phase: Phase,
    projectId: String,
    userPhoneNumber: String,
    expenseViewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val requests by expenseViewModel.phaseOverrideRequests.collectAsState()
    val isLoading by expenseViewModel.isLoading.collectAsState()
    val error by expenseViewModel.error.collectAsState()
    
    LaunchedEffect(projectId, phase.id, userPhoneNumber) {
        if (userPhoneNumber.isNotBlank()) {
            expenseViewModel.loadPhaseOverrideRequests(
                projectId = projectId,
                phaseId = phase.id,
                userPhoneNumber = userPhoneNumber
            )
        }
    }
    
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White, // White background
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .background(Color.White)
        ) {
            // Header with "Request Status" and "Done" button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Request Status",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E) // Dark grey instead of pure black
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Done",
                        fontSize = 17.sp,
                        color = Color(0xFF007AFF), // Blue color
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF007AFF))
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "Error loading requests",
                        color = Color(0xFFFF3B30),
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Request Summary
                    item {
                        Text(
                            text = "Request Summary",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E8E93), // Light gray
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7)), // Light grey card
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Requests",
                                    fontSize = 16.sp,
                                    color = Color(0xFF1C1C1E) // Dark grey text on light grey background
                                )
                                Text(
                                    text = "${requests.size}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E) // Dark grey text on light grey background
                                )
                            }
                        }
                    }
                    
                    // Request Details
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Request Details",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E8E93), // Light gray
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    // Request Items
                    items(requests) { request ->
                        RequestStatusItem(
                            request = request,
                            dateFormatter = dateFormatter
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestStatusItem(
    request: Map<String, Any>,
    dateFormatter: SimpleDateFormat
) {
    val status = request["status"] as? String ?: "PENDING"
    val reason = request["reason"] as? String ?: ""
    val extendedDateRaw = request["extendedDate"] as? String ?: ""
    val createdAt = request["createdAt"] as? Timestamp
    
    // Format extended date - it's stored as dd/MM/yyyy, display as is
    val extendedDate = extendedDateRaw
    
    val statusColor = when (status.uppercase()) {
        "ACCEPTED", "APPROVED" -> Color(0xFF34C759) // Green
        "REJECTED" -> Color(0xFFFF3B30) // Red
        else -> Color(0xFFFFCC00) // Yellow for PENDING
    }
    
    val statusIcon = when (status.uppercase()) {
        "ACCEPTED", "APPROVED" -> Icons.Default.CheckCircle
        "REJECTED" -> Icons.Default.Close
        else -> null
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7)), // Light grey card
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Status badge with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (statusIcon != null) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = status.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White, // White text for all status chips
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Reason
            if (reason.isNotBlank()) {
                Column {
                    Text(
                        text = "Reason",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93), // Light gray
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = reason,
                        fontSize = 15.sp,
                        color = Color(0xFF1C1C1E), // Dark grey text on light grey background
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            // Extension Date
            if (extendedDate.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color(0xFF007AFF), // Blue
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Extend to: $extendedDate",
                        fontSize = 14.sp,
                        color = Color(0xFF1C1C1E) // Dark grey text on light grey background
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Created Date (on the right)
            if (createdAt != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = dateFormatter.format(createdAt.toDate()),
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93) // Light gray
                    )
                }
            }
        }
    }
}

