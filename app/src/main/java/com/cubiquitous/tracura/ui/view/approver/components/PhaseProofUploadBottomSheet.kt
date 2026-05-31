package com.cubiquitous.tracura.ui.view.approver.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.cubiquitous.tracura.model.Phase
import com.cubiquitous.tracura.viewmodel.PhaseCompletionViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseProofUploadBottomSheet(
    phase: Phase,
    viewModel: PhaseCompletionViewModel,
    projectId: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Selected file state
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileExtension by remember { mutableStateOf<String?>(null) }

    // ViewModel state
    val isUploading = viewModel.isUploading
    val uploadProgress = viewModel.uploadProgress

    // Temp file URI for camera capture
    val cameraImageFile = remember {
        File(context.cacheDir, "phase_proof_temp.jpg")
    }
    val cameraImageUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cameraImageFile
        )
    }

    // Helper to decode bitmap from URI
    fun decodeBitmap(uri: Uri): android.graphics.Bitmap? = try {
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        }
    } catch (e: Exception) { null }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            selectedFileUri = null
            selectedFileName = null
            selectedFileExtension = null
            selectedImageBitmap = decodeBitmap(it)
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = cameraImageUri
            selectedFileUri = null
            selectedFileName = null
            selectedFileExtension = null
            selectedImageBitmap = decodeBitmap(cameraImageUri)
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(cameraImageUri)
    }

    // File picker launcher (PDF / doc)
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            selectedImageUri = null
            selectedImageBitmap = null
            val mimeType = context.contentResolver.getType(it) ?: ""
            selectedFileExtension = when {
                mimeType.contains("pdf") -> "pdf"
                mimeType.contains("word") -> "doc"
                else -> it.lastPathSegment?.substringAfterLast(".", "file") ?: "file"
            }
            selectedFileName = it.lastPathSegment?.substringAfterLast("/")
                ?: "Selected file"
        }
    }

    val hasSelection = selectedImageUri != null || selectedFileUri != null
    val canComplete = hasSelection && !isUploading

    ModalBottomSheet(
        onDismissRequest = { if (!isUploading) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Proof required to complete this phase",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Title
            Text(
                text = "Upload Phase Proof",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Subtitle
            Text(
                text = "Complete Phase: ${phase.phaseName}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            HorizontalDivider()

            // Preview area (shown only after selection)
            if (selectedImageBitmap != null) {
                Image(
                    bitmap = selectedImageBitmap!!.asImageBitmap(),
                    contentDescription = "Selected proof",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(12.dp)
                        )
                )
            } else if (selectedFileUri != null) {
                // File preview card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = selectedFileName ?: "Selected file",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                        Text(
                            text = selectedFileExtension?.uppercase() ?: "FILE",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // File picker buttons — hidden once a file is selected
            if (!hasSelection) {
                // Gallery
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Choose from Gallery")
                }

                // Camera
                OutlinedButton(
                    onClick = {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCameraPermission) {
                            cameraLauncher.launch(cameraImageUri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Take Photo")
                }

                // Files (PDF/doc)
                OutlinedButton(
                    onClick = {
                        fileLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "image/*"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Choose File")
                }
            } else {
                // Change file option
                TextButton(
                    onClick = {
                        selectedImageUri = null
                        selectedFileUri = null
                        selectedImageBitmap = null
                        selectedFileName = null
                        selectedFileExtension = null
                    }
                ) {
                    Text("Change file")
                }
            }

            // Upload progress
            if (isUploading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Uploading… ${(uploadProgress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.resetState()
                        onDismiss()
                    },
                    enabled = !isUploading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        viewModel.completePhaseWithProof(
                            projectId = projectId,
                            phase = phase,
                            imageUri = selectedImageUri,
                            fileUri = selectedFileUri,
                            context = context,
                            onSuccess = { onSuccess() },
                            onError = { /* handled via isUploading reset */ }
                        )
                    },
                    enabled = canComplete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = "Complete Now",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
