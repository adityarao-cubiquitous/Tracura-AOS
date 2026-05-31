package com.cubiquitous.tracura.ui.view.approver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.isSystemInDarkTheme
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

private data class ExpenseChatColors(
    val appBackground: Color,
    val cardSurface: Color,
    val fieldSurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color,
    val topBarBackground: Color,
    val topBarText: Color,
    val pendingStatus: Color,
    val approvedStatus: Color,
    val rejectedStatus: Color,
    val completeStatus: Color,
    val senderBubble: Color,
    val receiverBubble: Color,
    val danger: Color,
)

@Composable
private fun rememberExpenseChatColors(): ExpenseChatColors {
    val isDark = isSystemInDarkTheme()
    val accent = MaterialTheme.colorScheme.primary
    return remember(isDark, accent) {
        ExpenseChatColors(
            appBackground = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
            cardSurface = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            fieldSurface = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF),
            primaryText = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
            secondaryText = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43),
            accent = accent,
            topBarBackground = if (isDark) Color(0xFF1C1C1E) else accent,
            topBarText = Color.White,
            pendingStatus = if (isDark) Color(0xFFFFB74D) else Color(0xFFFF9800),
            approvedStatus = if (isDark) Color(0xFF90CAF9) else Color(0xFF1976D2),
            rejectedStatus = if (isDark) Color(0xFFEF9A9A) else Color(0xFFF44336),
            completeStatus = if (isDark) Color(0xFFA5D6A7) else Color(0xFF4CAF50),
            senderBubble = if (isDark) Color(0xFF2C2C2E) else Color(0xFFDCF8C6),
            receiverBubble = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
            danger = if (isDark) Color(0xFFFF6B6B) else Color(0xFFD32F2F),
        )
    }
}

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
fun ApproverExpenseChatScreen(
    expenseId: String,
    onNavigateBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    approvalViewModel: ApprovalViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val colors = rememberExpenseChatColors()
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
            val fileName = "approver_chat_${timestamp}.jpg"
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

    // Load messages as soon as we have projectId from expense
    // This listener setup is fast and non-blocking - it just sets up a real-time listener
    val projectId = selectedExpense?.projectId
    LaunchedEffect(expenseChatId, projectId) {
        if (projectId != null) {
            Log.d("ApproverExpenseChatScreen", "🔔 Setting up REAL-TIME message listener for project: $projectId, chat: $expenseChatId")
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
                            text = "Expense Approval Chat",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.topBarText
                        )
                        Text(
                            text = "Expense #${expenseId.takeLast(3).uppercase()}",
                            fontSize = 12.sp,
                            color = colors.topBarText.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.topBarText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.topBarBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.appBackground)
                .padding(paddingValues)
        ) {
            // Expense Details Header - Show loading state while expense loads
            if (isLoadingExpense && selectedExpense == null) {
                // Show loading skeleton
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.cardSurface),
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
                            color = colors.accent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading expense details...",
                            fontSize = 12.sp,
                            color = colors.secondaryText
                        )
                    }
                }
            } else {
                selectedExpense?.let { expense ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.cardSurface),
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
                                color = colors.accent
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Submitted by: ${expense.submittedBy}",
                                fontSize = 14.sp,
                                color = colors.secondaryText
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Date: ${expense.date?.let { FormatUtils.formatDate(it) } ?: "N/A"}",
                                fontSize = 14.sp,
                                color = colors.secondaryText
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Amount: ${FormatUtils.formatCurrency(expense.amount)}",
                                fontSize = 14.sp,
                                color = colors.secondaryText
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Status: ${expense.status.name}",
                                fontSize = 14.sp,
                                color = when (expense.status) {
                                    com.cubiquitous.tracura.model.ExpenseStatus.PENDING -> colors.pendingStatus
                                    com.cubiquitous.tracura.model.ExpenseStatus.APPROVED -> colors.approvedStatus
                                    com.cubiquitous.tracura.model.ExpenseStatus.REJECTED -> colors.rejectedStatus
                                    com.cubiquitous.tracura.model.ExpenseStatus.COMPLETE -> colors.completeStatus
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatState.messages) { message ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = currentUser?.let { message.senderId == it.getChatIdentifier() } ?: false
                    )
                }
            }

            // Message Input
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.cardSurface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Plus button
                    IconButton(
                        onClick = { showImagePicker = true }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add attachment",
                            tint = if (selectedImageUri != null) colors.accent else colors.secondaryText,
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
                                .background(colors.fieldSurface)
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
                                        .background(Color.Black.copy(alpha = if (isSystemInDarkTheme()) 0.35f else 0.5f))
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
                                            Color.Black.copy(alpha = if (isSystemInDarkTheme()) 0.5f else 0.7f),
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
                        placeholder = { Text("Type a message", color = colors.secondaryText) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.secondaryText.copy(alpha = 0.45f),
                            focusedContainerColor = colors.fieldSurface,
                            unfocusedContainerColor = colors.fieldSurface,
                            focusedTextColor = colors.primaryText,
                            unfocusedTextColor = colors.primaryText,
                            focusedPlaceholderColor = colors.secondaryText,
                            unfocusedPlaceholderColor = colors.secondaryText,
                            cursorColor = colors.accent
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            Log.d("ApproverExpenseChatScreen", "Send button clicked")
                            Log.d("ApproverExpenseChatScreen", "currentUser: $currentUser")
                            Log.d("ApproverExpenseChatScreen", "isUploadingImage: $isUploadingImage")
                            Log.d("ApproverExpenseChatScreen", "selectedExpense: $selectedExpense")
                            Log.d("ApproverExpenseChatScreen", "messageText: '$messageText'")
                            Log.d("ApproverExpenseChatScreen", "selectedImageUri: $selectedImageUri")
                            
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
                                    
                                    Log.d("ApproverExpenseChatScreen", "Sending text message: '$messageToSend'")
                                    Log.d("ApproverExpenseChatScreen", "Project ID: ${selectedExpense!!.projectId}")
                                    Log.d("ApproverExpenseChatScreen", "Chat ID: $expenseChatId")
                                    Log.d("ApproverExpenseChatScreen", "Sender: ${currentUser.name} (${currentUser.phone})")
                                    
                                    chatViewModel.sendMessage(
                                        projectId = selectedExpense!!.projectId,
                                        chatId = expenseChatId,
                                        senderId = currentUser.getChatIdentifier(),
                                        senderName = currentUser.name,
                                        senderRole = currentUser.getFormattedSenderRole(),
                                        message = messageToSend,
                                        context = context
                                    )
                                    Log.d("ApproverExpenseChatScreen", "Message sent, cleared input")
                                } else {
                                    Log.d("ApproverExpenseChatScreen", "No message to send - messageText is blank")
                                }
                            } else {
                                Log.d("ApproverExpenseChatScreen", "Send conditions not met:")
                                Log.d("ApproverExpenseChatScreen", "  currentUser != null: ${currentUser != null}")
                                Log.d("ApproverExpenseChatScreen", "  !isUploadingImage: ${!isUploadingImage}")
                                Log.d("ApproverExpenseChatScreen", "  selectedExpense != null: ${selectedExpense != null}")
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUploadingImage) 
                                    colors.secondaryText
                                else if (messageText.isNotBlank() || selectedImageUri != null) 
                                    colors.accent
                                else 
                                    colors.secondaryText.copy(alpha = 0.35f)
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
                        colors = CardDefaults.cardColors(containerColor = colors.danger.copy(alpha = 0.14f)),
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
                                tint = colors.danger,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                fontSize = 12.sp,
                                color = colors.danger
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { uploadError = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = colors.danger,
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
            containerColor = colors.cardSurface
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
                    color = colors.primaryText,
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
                            tint = colors.accent
                        )
                        Text(
                            text = "Camera",
                            fontSize = 14.sp,
                            color = colors.primaryText,
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
                            tint = colors.accent
                        )
                        Text(
                            text = "Gallery",
                            fontSize = 14.sp,
                            color = colors.primaryText,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean
) {
    val colors = rememberExpenseChatColors()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) colors.senderBubble else colors.receiverBubble,
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (!isCurrentUser) {
                    Text(
                        text = message.senderId,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (message.messageType == "Media" && message.mediaUrl != null) {
                    // Display image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp, max = 300.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.fieldSurface)
                    ) {
                        val painter = rememberAsyncImagePainter(
                            model = message.mediaUrl,
                            onLoading = { state ->
                                // Loading state handled by AsyncImagePainter
                            },
                            onError = { state ->
                                // Error state handled by AsyncImagePainter
                            }
                        )
                        
                        Image(
                            painter = painter,
                            contentDescription = "Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Text(
                        text = message.message,
                        fontSize = 16.sp,
                        color = colors.primaryText
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMessageTime(message.timestampTimestamp?.toDate()),
                        fontSize = 11.sp,
                        color = colors.secondaryText
                    )

                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.Done else Icons.Default.Check,
                            contentDescription = if (message.isRead) "Read" else "Sent",
                            tint = if (message.isRead) colors.accent else colors.secondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatMessageTime(date: Date?): String {
    if (date == null) return ""
    
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { time = date }
    
    return when {
        now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) -> {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
        }
        now.get(Calendar.DATE) - messageTime.get(Calendar.DATE) == 1 -> {
            "Yesterday ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)}"
        }
        else -> {
            SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(date)
        }
    }
}
