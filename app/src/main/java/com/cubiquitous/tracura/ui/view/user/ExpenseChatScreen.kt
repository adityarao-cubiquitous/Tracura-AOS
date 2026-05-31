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
import kotlinx.coroutines.launch
import com.cubiquitous.tracura.model.Message
import com.cubiquitous.tracura.model.ExpenseChat
import com.cubiquitous.tracura.model.Project
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.ChatViewModel
import com.cubiquitous.tracura.utils.ImageUriHelper
import com.cubiquitous.tracura.utils.FormatUtils
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
            topBarText = if (isDark) Color(0xFFFFFFFF) else Color(0xFFFFFFFF),
            pendingStatus = if (isDark) Color(0xFFFFB74D) else Color(0xFFFF9800),
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
fun ExpenseChatScreen(
    project: Project,
    onNavigateBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val colors = rememberExpenseChatColors()
    val authState by authViewModel.authState.collectAsState()
    val chatState by chatViewModel.chatState.collectAsState()
    val expenseChats by chatViewModel.expenseChats.collectAsState()
    val currentUser = authState.user

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showImagePicker by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun handleSelectedUri(uri: Uri?) {
        if (uri == null) {
            uploadError = "No image selected"
            return
        }
        selectedImageUri = uri
        uploadError = null
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                uploadError = "Selected file is not accessible"
            } else {
                inputStream.close()
            }
        } catch (e: Exception) {
            uploadError = "Error accessing selected file: ${e.message}"
        }
    }

    // Fallback gallery picker for devices with flaky Photo Picker behavior (seen on some Samsung builds)
    val galleryFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            handleSelectedUri(uri)
        } else {
            uploadError = "No image selected"
        }
    }

    // Create a unique chat ID for expense approval chat  
    val expenseChatId = "expense_approval_${project.id}"
    // Extract expense ID from chat ID (format: "expense_approval_{expenseId}")
    val expenseId = expenseChatId.removePrefix("expense_approval_")

    // Image picker launcher for gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            handleSelectedUri(uri)
        } else {
            // Fallback for devices where PickVisualMedia intermittently returns null
            galleryFallbackLauncher.launch("image/*")
        }
    }
    
    // File picker launcher for PDFs, docs, etc.
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { fileUri ->
            selectedImageUri = fileUri
            uploadError = null
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                if (inputStream == null) {
                    uploadError = "Selected file is not accessible"
                } else {
                    inputStream.close()
                }
            } catch (e: Exception) {
                uploadError = "Error accessing selected file: ${e.message}"
            }
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
            val fileName = "expense_chat_${timestamp}.jpg"
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

    // Load expenseChats using getExpenseChats() which uses addSnapshotListener for fast real-time updates
    // This listens directly to expenses/{expenseId}/expenseChats collection with real-time updates
    // Path: customers/{customerId}/projects/{projectId}/expenses/{expenseId}/expenseChats
    LaunchedEffect(expenseId) {
        val projectId = project.id ?: return@LaunchedEffect
        if (expenseId.isNotEmpty()) {
            chatViewModel.loadExpenseChats(projectId, expenseId)
        }
    }

    // Convert ExpenseChat to Message for display compatibility
    val messages = remember(expenseChats) {
        expenseChats.map { expenseChat ->
            Message(
                id = expenseChat.id,
                chatId = expenseChatId,
                senderId = expenseChat.senderId,
                isGroupMessage = false,
                isRead = false,
                text = expenseChat.textMessage,
                timeStamp = expenseChat.timeStamp?.let { FormatUtils.formatMessageTimestamp(it) } ?: "",
                type = if (expenseChat.mediaURL.isNotEmpty()) "media" else "text",
                messageType = if (expenseChat.mediaURL.isNotEmpty()) "Media" else "Text",
                message = expenseChat.textMessage,
                mediaUrl = expenseChat.mediaURL.firstOrNull(),
                timestampTimestamp = expenseChat.timeStamp,
                senderName = "", // Will be populated if needed
                senderRole = expenseChat.senderRole
            )
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
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
                            text = project.name,
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
                .imePadding()
        ) {
            // Expense Details Header
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
//                    Text(
//                        text = "EXPENSE ID: #EXP${(project.id ?: "").takeLast(3).uppercase()} - ${project.name}",
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color(0xFF4285F4)
//                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Submitted by: ${currentUser?.name ?: "Unknown"}",
                        fontSize = 14.sp,
                        color = colors.secondaryText
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}",
                        fontSize = 14.sp,
                        color = colors.secondaryText
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Status: Pending Approval",
                        fontSize = 14.sp,
                        color = colors.pendingStatus,
                        fontWeight = FontWeight.Medium
                    )
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
                items(messages) { message ->
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
                            tint = if (selectedImageUri != null) colors.accent else colors.secondaryText,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Attachment Preview Thumbnail
                    selectedImageUri?.let { uri ->
                        val selectedMimeType = context.contentResolver.getType(uri)
                        val isSelectedImage = selectedMimeType?.startsWith("image/") == true
                        
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.fieldSurface)
                        ) {
                            if (isSelectedImage) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = "Selected image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Show file icon for non-image files
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.InsertDriveFile,
                                        contentDescription = "File",
                                        tint = colors.accent,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            
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
                            
                            // Clear attachment button
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
                                        contentDescription = "Remove",
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
                            val projectId = project.id
                            if (currentUser != null && !isUploadingImage && projectId != null) {
                                if (selectedImageUri != null) {
                                    // Send attachment
                                    isUploadingImage = true
                                    uploadError = null
                                    
                                    try {
                                        chatViewModel.sendImageMessageAsync(
                                            projectId = projectId,
                                            chatId = expenseChatId,
                                            senderId = currentUser.getChatIdentifier(),
                                            senderName = currentUser.name,
                                            senderRole = currentUser.getFormattedSenderRole(),
                                            imageUri = selectedImageUri!!
                                        )
                                        
                                        selectedImageUri = null
                                        cameraImageUri = null
                                        uploadError = null
                                        isUploadingImage = false
                                    } catch (e: Exception) {
                                        uploadError = "Couldn't send attachment: ${e.message}"
                                        isUploadingImage = false
                                    }
                                } else if (messageText.isNotBlank()) {
                                    // Send text message
                                    chatViewModel.sendMessage(
                                        projectId = projectId,
                                        chatId = expenseChatId,
                                        senderId = currentUser.getChatIdentifier(),
                                        senderName = currentUser.name,
                                        senderRole = currentUser.getFormattedSenderRole(),
                                        message = messageText,
                                        context = context
                                    )
                                    messageText = ""
                                }
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
                    text = "Attach",
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

                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
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
                
                // File option
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            showImagePicker = false
                            filePickerLauncher.launch("*/*")
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "File",
                        modifier = Modifier
                            .size(48.dp)
                            .padding(8.dp),
                        tint = colors.accent
                    )
                    Text(
                        text = "File (PDF, Doc, etc.)",
                        fontSize = 14.sp,
                        color = colors.primaryText,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
    val context = LocalContext.current
    val colors = rememberExpenseChatColors()
    
    // Determine if this is a media message
    val isMediaMessage = (message.messageType == "Media" || message.type == "media") && 
        !message.mediaUrl.isNullOrEmpty()
    
    // Determine if the URL points to an image
    val isImageUrl = message.mediaUrl?.let { url ->
        url.contains(".jpg", ignoreCase = true) || url.contains(".jpeg", ignoreCase = true) ||
        url.contains(".png", ignoreCase = true) || url.contains(".gif", ignoreCase = true) ||
        url.contains(".webp", ignoreCase = true) || url.contains("chat_images", ignoreCase = true)
    } ?: false

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
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Sender name for received messages
                if (!isCurrentUser && message.senderName.isNotEmpty()) {
                    Text(
                        text = message.senderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (isMediaMessage) {
                    if (isImageUrl) {
                        // Image message — show image with loading state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp, max = 300.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.fieldSurface)
                                .clickable {
                                    message.mediaUrl?.let { url ->
                                        ImageUriHelper.openFirebaseStorageFile(
                                            context = context,
                                            storageUrl = url,
                                            onError = { errorMsg ->
                                                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                        ) {
                            val painter = rememberAsyncImagePainter(model = message.mediaUrl)
                            
                            Image(
                                painter = painter,
                                contentDescription = "Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Loading overlay
                            if (painter.state is coil.compose.AsyncImagePainter.State.Loading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            
                            // Error fallback
                            if (painter.state is coil.compose.AsyncImagePainter.State.Error) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.BrokenImage,
                                            contentDescription = null,
                                            tint = colors.secondaryText,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Tap to view",
                                            fontSize = 12.sp,
                                            color = colors.secondaryText
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // File attachment — styled card
                        val fileLabel = message.message.ifEmpty { message.text.ifEmpty { "📎 File" } }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    message.mediaUrl?.let { url ->
                                        ImageUriHelper.openFirebaseStorageFile(
                                            context = context,
                                            storageUrl = url,
                                            onError = { errorMsg ->
                                                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                            color = colors.fieldSurface,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // File type icon
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.accent.copy(alpha = 0.16f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fileLabel,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.primaryText,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Tap to open",
                                        fontSize = 11.sp,
                                        color = colors.secondaryText
                                    )
                                }
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Text message
                    Text(
                        text = message.message.ifEmpty { message.text },
                        fontSize = 16.sp,
                        color = colors.primaryText
                    )
                }

                // Timestamp
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
                            modifier = Modifier.size(14.dp)
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
