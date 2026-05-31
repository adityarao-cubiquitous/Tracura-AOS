package com.cubiquitous.tracura.ui.view.common

import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.Message
import com.cubiquitous.tracura.model.User
import com.cubiquitous.tracura.viewmodel.AuthViewModel
import com.cubiquitous.tracura.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import androidx.core.app.NotificationCompat
import com.cubiquitous.tracura.R
import com.cubiquitous.tracura.MainActivity
import com.cubiquitous.tracura.utils.ImageUriHelper
import kotlinx.coroutines.launch

/**
 * Get chat identifier for user (phone number for regular users, "BusinessHead" for BusinessHead)
 */
private fun User.getChatIdentifier(): String {
    return if (this.role.name != "BUSINESS_HEAD") this.phone else "BusinessHead"
}

fun showMessageNotification(context: Context, senderName: String, message: String) {
    try {
        val channelId = "message_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Message Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New message from $senderName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    } catch (e: Exception) {
        android.util.Log.e("ChatScreen", "Error showing notification: ${e.message}", e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    projectId: String,
    chatId: String,
    otherUserName: String,
    onNavigateBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val chatState by chatViewModel.chatState.collectAsState()
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
            try {
                if (ImageUriHelper.isUriAccessible(context, cameraImageUri!!)) {
                    uploadError = null
                } else {
                    uploadError = "Camera image is not accessible"
                }
            } catch (e: Exception) {
                uploadError = "Error accessing camera file: ${e.message}"
            }
        } else {
            uploadError = "Camera capture failed"
        }
    }

    fun launchCamera() {
        try {
            val timestamp = System.currentTimeMillis()
            val fileName = "camera_image_${timestamp}.jpg"
            val photoUri = ImageUriHelper.createCameraImageUri(context, fileName)
            if (photoUri != null) {
                if (ImageUriHelper.isUriAccessible(context, photoUri)) {
                    cameraImageUri = photoUri
                    cameraLauncher.launch(photoUri)
                } else {
                    uploadError = "Cannot access camera image file"
                }
            } else {
                uploadError = "Failed to create camera image file"
            }
        } catch (e: Exception) {
            uploadError = "Error setting up camera: ${e.message}"
        }
    }

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

    LaunchedEffect(chatId) {
        chatViewModel.loadMessages(projectId, chatId)
    }

    LaunchedEffect(chatId) {
        currentUser?.let { user ->
            val userIdentifier = user.getChatIdentifier()
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    chatViewModel.markMessagesAsRead(projectId, chatId, userIdentifier)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Error marking messages as read: ${e.message}", e)
            }
        }
    }

    LaunchedEffect(chatId, chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            kotlinx.coroutines.delay(500)
            currentUser?.let { user ->
                val userIdentifier = user.getChatIdentifier()
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                        chatViewModel.markMessagesAsRead(projectId, chatId, userIdentifier)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatScreen", "Error marking messages as read: ${e.message}", e)
                }
            }
        }
    }

    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = otherUserName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        chatState.currentChatUser?.let { user ->
                            Text(
                                text = if (user.isOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user.isOnline)
                                    Color(0xFF4CAF50)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chatState.messages) { message ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = currentUser?.let { message.senderId == it.getChatIdentifier() } ?: false
                    )
                }
            }

            // Upload error banner
            uploadError?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { uploadError = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Message Input Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment button
                    IconButton(onClick = { showImagePicker = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add attachment",
                            tint = if (selectedImageUri != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Attachment preview thumbnail
                    selectedImageUri?.let { uri ->
                        val selectedMimeType = context.contentResolver.getType(uri)
                        val isSelectedImage = selectedMimeType?.startsWith("image/") == true
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            if (isUploadingImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            if (!isUploadingImage) {
                                IconButton(
                                    onClick = {
                                        selectedImageUri = null
                                        cameraImageUri = null
                                        uploadError = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(18.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Text field
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Message",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send button
                    val canSend = !isUploadingImage && (messageText.isNotBlank() || selectedImageUri != null)
                    FilledIconButton(
                        onClick = {
                            if (currentUser != null && !isUploadingImage) {
                                if (selectedImageUri != null) {
                                    val uriToSend = selectedImageUri!!
                                    selectedImageUri = null
                                    cameraImageUri = null
                                    uploadError = null
                                    isUploadingImage = true
                                    coroutineScope.launch {
                                        try {
                                            val success = chatViewModel.sendImageMessage(
                                                projectId = projectId,
                                                chatId = chatId,
                                                senderId = currentUser.getChatIdentifier(),
                                                senderName = currentUser.name,
                                                senderRole = currentUser.role.name,
                                                imageUri = uriToSend
                                            )
                                            isUploadingImage = false
                                            if (!success) {
                                                uploadError = "Couldn't send attachment. Try again."
                                            }
                                        } catch (e: Exception) {
                                            isUploadingImage = false
                                            uploadError = "Couldn't send attachment: ${e.message}"
                                        }
                                    }
                                } else if (messageText.isNotBlank()) {
                                    val messageToSend = messageText
                                    messageText = ""
                                    chatViewModel.sendMessage(
                                        projectId = projectId,
                                        chatId = chatId,
                                        senderId = currentUser.getChatIdentifier(),
                                        senderName = currentUser.name,
                                        senderRole = currentUser.role.name,
                                        message = messageToSend,
                                        context = context
                                    )
                                }
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier.size(44.dp)
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Image Picker Bottom Sheet
    if (showImagePicker) {
        ModalBottomSheet(
            onDismissRequest = { showImagePicker = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Attach",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Camera
                    AttachmentOption(
                        icon = Icons.Default.CameraAlt,
                        label = "Camera",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showImagePicker = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                launchCamera()
                            } else {
                                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                            }
                        }
                    )

                    // Gallery
                    AttachmentOption(
                        icon = Icons.Default.PhotoLibrary,
                        label = "Gallery",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showImagePicker = false
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    // File
                    AttachmentOption(
                        icon = Icons.Default.AttachFile,
                        label = "File",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showImagePicker = false
                            filePickerLauncher.launch("*/*")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean
) {
    val bubbleColor = if (isCurrentUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    val textColor = if (isCurrentUser)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    val context = androidx.compose.ui.platform.LocalContext.current
    
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
            color = bubbleColor,
            tonalElevation = if (isCurrentUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                // Sender name for received messages
                if (!isCurrentUser && message.senderName.isNotEmpty()) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                if (isMediaMessage) {
                    if (isImageUrl) {
                        // Image message — show image with loading state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp, max = 280.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            
                            // Error fallback — show as file card instead
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
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Tap to view",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = MaterialTheme.colorScheme.surfaceVariant,
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
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.InsertDriveFile,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fileLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Tap to open",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Text message
                    Text(
                        text = message.message.ifEmpty { message.text },
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }

                // Timestamp row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMessageTime(message.timestampTimestamp?.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentUser)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.Done else Icons.Default.Check,
                            contentDescription = if (message.isRead) "Read" else "Sent",
                            tint = if (message.isRead)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
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
        now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) ->
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
        now.get(Calendar.DATE) - messageTime.get(Calendar.DATE) == 1 ->
            "Yesterday ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)}"
        else ->
            SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(date)
    }
}
