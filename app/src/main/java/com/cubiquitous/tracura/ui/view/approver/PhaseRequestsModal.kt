package com.cubiquitous.tracura.ui.view.approver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.viewmodel.ApproverProjectViewModel
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PhaseRequestsModal(
    projectId: String,
    onDismiss: () -> Unit,
    onRequestClick: (Map<String, Any>) -> Unit = {},
    viewModel: ApproverProjectViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onRequestProcessed: () -> Unit = {}
) {
    val phaseRequests by viewModel.phaseRequests.collectAsState()
    val pendingCount = phaseRequests.size
    var selectedRequest by remember { mutableStateOf<Map<String, Any>?>(null) }
    
    // Load phase requests when modal opens (only if not already loaded)
    LaunchedEffect(projectId) {
        if (phaseRequests.isEmpty()) {
            viewModel.loadPhaseRequests(projectId)
        }
    }
    
    // Show detail view if request is selected
    if (selectedRequest != null) {
        PhaseRequestDetailModal(
            projectId = projectId,
            request = selectedRequest!!,
            onDismiss = { selectedRequest = null },
            onBack = { selectedRequest = null },
            viewModel = viewModel,
            authViewModel = authViewModel,
            onRequestProcessed = {
                selectedRequest = null
                viewModel.loadPhaseRequests(projectId) // Refresh list
                onRequestProcessed() // Notify parent to refresh count
            }
        )
        return
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Phase Requests",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$pendingCount pending",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Requests List
                if (phaseRequests.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pending requests",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(phaseRequests) { request ->
                            PhaseRequestItem(
                                request = request,
                                authViewModel = authViewModel,
                                onClick = { selectedRequest = request }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhaseRequestItem(
    request: Map<String, Any>,
    authViewModel: AuthViewModel,
    onClick: () -> Unit
) {
    var userName by remember { mutableStateOf<String?>(null) }
    val userID = request["userID"] as? String ?: ""
    val extendedDate = request["extendedDate"] as? String ?: ""
    val reason = request["reason"] as? String ?: ""
    val phaseName = request["phaseName"] as? String ?: ""
    val createdAt = request["createdAt"] as? Timestamp
    
    // Get user name by phone number
    LaunchedEffect(userID) {
        if (userID.isNotEmpty()) {
            try {
                val user = authViewModel.getUserByPhoneNumber(userID)
                userName = user?.name
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    // Format dates
    val createdDateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val createdDateText = createdAt?.toDate()?.let { createdDateFormatter.format(it) } ?: ""
    val displayName = userName ?: userID
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Clock icon with phase name horizontally
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Orange clock icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9800)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Phase name
                Text(
                    text = phaseName.ifEmpty { "Phase" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Right arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Content below icon and phase name
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 64.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // User name
            Text(
                text = displayName,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Phone number
            Text(
                text = userID,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Reason
            Text(
                text = reason.ifEmpty { "Check" },
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Extended date with calendar icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Extend to: $extendedDate",
                    fontSize = 14.sp,
                    color = Color(0xFF4285F4)
                )
            }
            
            // Created date
            Text(
                text = createdDateText,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun PhaseRequestDetailModal(
    projectId: String,
    request: Map<String, Any>,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    viewModel: ApproverProjectViewModel,
    authViewModel: AuthViewModel,
    onRequestProcessed: () -> Unit
) {
    var userName by remember { mutableStateOf<String?>(null) }
    var responseReason by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    
    val userID = request["userID"] as? String ?: ""
    val extendedDate = request["extendedDate"] as? String ?: ""
    val reason = request["reason"] as? String ?: ""
    val phaseName = request["phaseName"] as? String ?: ""
    val phaseId = request["phaseId"] as? String ?: ""
    val requestId = request["id"] as? String ?: ""
    val createdAt = request["createdAt"] as? Timestamp
    
    // Get user name by phone number
    LaunchedEffect(userID) {
        if (userID.isNotEmpty()) {
            try {
                val user = authViewModel.getUserByPhoneNumber(userID)
                userName = user?.name
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    // Format dates
    val createdDateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val requestedOnText = createdAt?.toDate()?.let { createdDateFormatter.format(it) } ?: ""
    val displayName = userName ?: userID
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF007AFF),
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "Phase Request",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    // Spacer to balance the layout
                    Spacer(modifier = Modifier.width(64.dp))
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // REQUEST DETAILS Section
                    Text(
                        text = "REQUEST DETAILS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF8E8E93),
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Divider(color = Color(0xFFE5E5EA), thickness = 1.dp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Phase
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Phase",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Text(
                            text = phaseName.ifEmpty { "Phase" },
                            fontSize = 16.sp,
                            color = Color.Black,
                            textAlign = TextAlign.End
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Requested By
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Requested By",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = displayName,
                                fontSize = 16.sp,
                                color = Color.Black,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = userID,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Extend To
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Extend To",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Text(
                            text = extendedDate,
                            fontSize = 16.sp,
                            color = Color(0xFF007AFF),
                            textAlign = TextAlign.End
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Requested On
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Requested On",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Text(
                            text = requestedOnText,
                            fontSize = 16.sp,
                            color = Color.Black,
                            textAlign = TextAlign.End
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Reason
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Reason",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.widthIn(max = 200.dp)
                        ) {
                            Text(
                                text = reason.ifEmpty { "Check" },
                                fontSize = 16.sp,
                                color = Color.Black,
                                textAlign = TextAlign.End,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // YOUR RESPONSE Section
                    Text(
                        text = "YOUR RESPONSE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF8E8E93),
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Divider(color = Color(0xFFE5E5EA), thickness = 1.dp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Reason for Accept/Reject label
                    Text(
                        text = "Reason for Accept/Reject",
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Text input field
                    OutlinedTextField(
                        value = responseReason,
                        onValueChange = { responseReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter reason", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFFC7C7CC),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = false,
                        maxLines = 5
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Reject button
                        Button(
                            onClick = {
                                isProcessing = true
                                viewModel.rejectPhaseRequest(
                                    projectId = projectId,
                                    phaseId = phaseId,
                                    requestId = requestId,
                                    reason = responseReason
                                ) {
                                    isProcessing = false
                                    onRequestProcessed()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF3B30)
                            ),
                            enabled = !isProcessing && responseReason.isNotBlank()
                        ) {
                            Text(
                                text = "Reject",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        
                        // Accept button
                        Button(
                            onClick = {
                                isProcessing = true
                                viewModel.approvePhaseRequest(
                                    projectId = projectId,
                                    phaseId = phaseId,
                                    requestId = requestId,
                                    extendedDate = extendedDate,
                                    reason = responseReason
                                ) {
                                    isProcessing = false
                                    onRequestProcessed()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF34C759)
                            ),
                            enabled = !isProcessing && responseReason.isNotBlank()
                        ) {
                            Text(
                                text = "Accept",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
