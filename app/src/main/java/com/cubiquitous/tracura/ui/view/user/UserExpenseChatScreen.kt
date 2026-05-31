package com.cubiquitous.tracura.ui.view.user

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import com.cubiquitous.tracura.model.Message
import com.cubiquitous.tracura.model.Expense
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.ChatViewModel
import com.cubiquitous.tracura.viewmodel.ApprovalViewModel
import com.cubiquitous.tracura.utils.ImageUriHelper
import com.cubiquitous.tracura.utils.FormatUtils
import com.cubiquitous.tracura.ui.view.approver.MessageBubble
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.*

/**
 * Get chat identifier for user (phone number for regular users, "BusinessHead" for BusinessHead)
 * Per guide: BUSINESSHEAD uses "BusinessHead", APPROVER/USER use phone number
 */
private fun User.getChatIdentifier(): String {
    return when (this.role) {
        com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD -> "BusinessHead"
        else -> this.phone
    }
}

/**
 * Format senderRole according to guide: "BUSINESSHEAD", "APPROVER", or "USER"
 * Converts from enum name (e.g., "BUSINESS_HEAD") to guide format (e.g., "BUSINESSHEAD")
 */
private fun User.getFormattedSenderRole(): String {
    return when (this.role) {
        com.cubiquitous.tracura.model.UserRole.BUSINESS_HEAD -> "BUSINESSHEAD"
        com.cubiquitous.tracura.model.UserRole.MANAGER -> "BUSINESSHEAD"
        com.cubiquitous.tracura.model.UserRole.APPROVER -> "APPROVER"
        com.cubiquitous.tracura.model.UserRole.USER -> "USER"
        com.cubiquitous.tracura.model.UserRole.ADMIN -> "ADMIN"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserExpenseChatScreen(
    expenseId: String,
    onNavigateBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    approvalViewModel: ApprovalViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val chatState by chatViewModel.chatState.collectAsState()
    val selectedExpense by approvalViewModel.selectedExpense.collectAsState()
    val isLoadingExpense by approvalViewModel.isLoading.collectAsState()
    val currentUser = authState.user

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showImagePicker by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Create a unique chat ID for expense approval chat
    val expenseChatId = "expense_approval_${expenseId}"

    // OPTIMIZED: Fetch expense and load messages in parallel
    // Once expense loads, we get projectId and can start loading messages
    LaunchedEffect(expenseId) {
        // Start expense fetch immediately (non-blocking)
        approvalViewModel.fetchExpenseById(expenseId)
    }

    // Load messages as soon as we have projectId from expense
    // This listener setup is fast and non-blocking - it just sets up a real-time listener
    val projectId = selectedExpense?.projectId
    LaunchedEffect(expenseChatId, projectId) {
        if (projectId != null) {
            // Load messages immediately - this is fast (just sets up listener, doesn't block)
            chatViewModel.loadMessages(projectId, expenseChatId)
            
            // DEFERRED: Mark messages as read AFTER initial render (non-blocking, runs in background)
            // This prevents blocking the UI during initial load
            // Use the coroutineScope from rememberCoroutineScope() to launch a background job
            currentUser?.let { user ->
                coroutineScope.launch(Dispatchers.IO) {
                    // Wait a bit for messages to start loading before marking as read
                    delay(1000)
                    val userIdentifier = user.getChatIdentifier()
                    chatViewModel.markMessagesAsRead(projectId, expenseChatId, userIdentifier)
                }
            }
        }
    }

    // Image picker launcher for gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { imageUri ->
            selectedImageUri = imageUri
            uploadError = null
        }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            uploadError = null
        }
    }
    
    // Function to launch camera with permission check
    fun launchCamera() {
        try {
            val timestamp = System.currentTimeMillis()
            val fileName = "user_expense_chat_${timestamp}.jpg"
            val photoUri = ImageUriHelper.createCameraImageUri(context, fileName)
            
            if (photoUri != null && ImageUriHelper.isUriAccessible(context, photoUri)) {
                cameraImageUri = photoUri
                cameraLauncher.launch(photoUri)
            } else {
                uploadError = "Cannot access camera image file"
            }
        } catch (e: Exception) {
            uploadError = "Error setting up camera: ${e.message}"
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        val oldStorageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        
        if (cameraGranted) {
            launchCamera()
        } else if (storageGranted || oldStorageGranted) {
            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }


    // OPTIMIZED: Auto-scroll to bottom only when new messages are added (not on every recomposition)
    var previousMessageCount by remember { mutableStateOf(0) }
    LaunchedEffect(chatState.messages.size) {
        val currentCount = chatState.messages.size
        // Only scroll if we have messages and the count increased (new message added)
        if (currentCount > 0 && currentCount > previousMessageCount) {
            listState.animateScrollToItem(currentCount - 1)
        }
        previousMessageCount = currentCount
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Expense Chat1",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "Expense #${expenseId.takeLast(3).uppercase()}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4285F4)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
        ) {
            // Expense Details Header - Show loading state while expense loads
            if (isLoadingExpense && selectedExpense == null) {
                // Show loading skeleton
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF4285F4),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading expense details...",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                selectedExpense?.let { expense ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "EXPENSE ID: #EXP${expenseId.takeLast(3).uppercase()} - ${expense.category}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4285F4)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Amount: ${FormatUtils.formatCurrency(expense.amount)}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Date: ${expense.date?.let { FormatUtils.formatDate(it) } ?: "N/A"}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Status: ${expense.status.name}",
                                fontSize = 14.sp,
                                color = when (expense.status) {
                                    com.cubiquitous.tracura.model.ExpenseStatus.PENDING -> Color(0xFFFF9800)
                                    com.cubiquitous.tracura.model.ExpenseStatus.APPROVED -> Color(0xFF1976D2) // Blue
                                    com.cubiquitous.tracura.model.ExpenseStatus.REJECTED -> Color(0xFFF44336)
                                    com.cubiquitous.tracura.model.ExpenseStatus.COMPLETE -> Color(0xFF4CAF50) // Green
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Messages List
            // Use key to ensure proper recomposition when messages are updated in Firestore
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = chatState.messages,
                    key = { message -> "${message.id}_${message.text}_${message.timeStamp}" }
                ) { message ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = currentUser?.let { message.senderId == it.getChatIdentifier() } ?: false
                    )
                }
            }

            // Message Input
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Plus button
                    IconButton(
                        onClick = { showImagePicker = true }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add attachment",
                            tint = if (selectedImageUri != null) Color(0xFF4285F4) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Image Preview Thumbnail
                    selectedImageUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.1f))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Upload progress indicator
                            if (isUploadingImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            
                            // Clear image button
                            if (!isUploadingImage) {
                                IconButton(
                                    onClick = { 
                                        selectedImageUri = null
                                        cameraImageUri = null
                                        uploadError = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.7f),
                                            CircleShape
                                        )
                                        .padding(2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove image",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Message Input Field
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message1") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            
                            if (currentUser != null && !isUploadingImage && selectedExpense != null) {
                                if (selectedImageUri != null) {
                                    // Send image message
                                    isUploadingImage = true
                                    uploadError = null
                                    
                                    if (!ImageUriHelper.isUriAccessible(context, selectedImageUri!!)) {
                                        uploadError = "Selected image is not accessible"
                                        isUploadingImage = false
                                    } else {
                                        try {
                                            val imageUriToSend = selectedImageUri!!
                                            
                                            // Clear the image preview immediately - message appears via optimistic update
                                            selectedImageUri = null
                                            cameraImageUri = null
                                            uploadError = null
                                            isUploadingImage = false
                                            
                                            // Send image message - appears immediately in UI via optimistic update
                                            chatViewModel.sendImageMessageAsync(
                                                projectId = selectedExpense!!.projectId,
                                                chatId = expenseChatId,
                                                senderId = currentUser.getChatIdentifier(),
                                                senderName = currentUser.name,
                                                senderRole = currentUser.getFormattedSenderRole(),
                                                imageUri = imageUriToSend
                                            )
                                        } catch (e: Exception) {
                                            uploadError = "Error sending image: ${e.message}"
                                            isUploadingImage = false
                                        }
                                    }
                                } else if (messageText.isNotBlank()) {
                                    // Send text message - message appears immediately via optimistic update
                                    val messageToSend = messageText
                                    messageText = "" // Clear input immediately for better UX
                                    
                                    chatViewModel.sendMessage(
                                        projectId = selectedExpense!!.projectId,
                                        chatId = expenseChatId,
                                        senderId = currentUser.getChatIdentifier(),
                                        senderName = currentUser.name,
                                        senderRole = currentUser.getFormattedSenderRole(),
                                        message = messageToSend,
                                        context = context
                                    )
                                } else {
                                }
                            } else {
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUploadingImage) 
                                    Color.Gray
                                else if (messageText.isNotBlank() || selectedImageUri != null) 
                                    Color(0xFF4285F4) 
                                else 
                                    Color.LightGray
                            )
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                // Error message display
                uploadError?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { uploadError = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Image Picker Bottom Sheet
    if (showImagePicker) {
        ModalBottomSheet(
            onDismissRequest = { showImagePicker = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Image",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Camera option
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            showImagePicker = false
                            
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                launchCamera()
                            } else {
                                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            modifier = Modifier
                                .size(48.dp)
                                .padding(8.dp),
                            tint = Color(0xFF4285F4)
                        )
                        Text(
                            text = "Camera",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    // Gallery option
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            showImagePicker = false
                            
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                                }
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            modifier = Modifier
                                .size(48.dp)
                                .padding(8.dp),
                            tint = Color(0xFF4285F4)
                        )
                        Text(
                            text = "Gallery",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

