package com.cubiquitous.tracura.ui.view.user

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.cubiquitous.tracura.model.DepartmentLineItemData
import com.cubiquitous.tracura.model.MatchedLineItemResult
import com.cubiquitous.tracura.model.ReceiptResult
import com.cubiquitous.tracura.viewmodel.ReceiptScannerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ReceiptScannerSheet(
    onDismiss: () -> Unit,
    onResult: (result: ReceiptResult, imageUri: Uri?) -> Unit,
    candidateCategories: List<String> = emptyList(),
    candidateItems: List<String> = emptyList(),
    /** When non-empty, the line-item matching flow is used instead of the standard field-extraction flow. */
    departmentLineItems: List<DepartmentLineItemData> = emptyList(),
    /** Called when line-item matching completes (only when [departmentLineItems] is non-empty). */
    onLineItemsMatched: ((matches: List<MatchedLineItemResult>, imageUri: Uri?) -> Unit)? = null,
    viewModel: ReceiptScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val analysisProgress by viewModel.analysisProgress.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Helper: launch the right analysis depending on whether dept line items are available
    fun analyzeImage(bitmap: Bitmap) {
        if (departmentLineItems.isNotEmpty()) {
            viewModel.analyzeReceiptForLineItems(bitmap, departmentLineItems)
        } else {
            viewModel.analyzeReceipt(bitmap, candidateCategories, candidateItems)
        }
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                        .copy(Bitmap.Config.ARGB_8888, true)
                } else {
                    @Suppress("DEPRECATION")
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    analyzeImage(bitmap)
                }
            } catch (e: Exception) {
                Log.e("ReceiptScanner", "Gallery pick error: ${e.message}")
            }
        }
    }

    // State callback routing
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ReceiptScannerViewModel.ScannerUiState.Success -> {
                val imageUri = capturedBitmap?.let { saveBitmapToUri(context, it) }
                onResult(state.result, imageUri)
                viewModel.reset()
            }
            is ReceiptScannerViewModel.ScannerUiState.LineItemsMatched -> {
                val imageUri = capturedBitmap?.let { saveBitmapToUri(context, it) }
                onLineItemsMatched?.invoke(state.matches, imageUri)
                viewModel.reset()
            }
            else -> {}
        }
    }

    Dialog(
        onDismissRequest = {
            viewModel.reset()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when {
                !cameraPermission.status.isGranted -> {
                    // Permission screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Camera Permission Required",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (cameraPermission.status.shouldShowRationale)
                                "Camera access is needed to scan receipts. Please grant permission."
                            else
                                "Allow camera access to capture receipt images for scanning.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { cameraPermission.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                        ) {
                            Text("Grant Permission")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Text("Pick from Gallery instead", color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                else -> {
                    // Camera preview
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    imageCapture = capture
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        capture
                                    )
                                } catch (e: Exception) {
                                    Log.e("ReceiptScanner", "CameraX bind error: ${e.message}")
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.reset()
                        onDismiss()
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Scan Receipt",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                // Balance spacer
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Viewfinder guide overlay
            if (cameraPermission.status.isGranted) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1.6f),
                        color = Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.6f))
                    ) {}
                    Text(
                        text = "Align receipt within the frame",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 180.dp)
                    )
                }
            }

            // Bottom action bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .navigationBarsPadding()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            IconButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Default.Photo,
                                    contentDescription = "Gallery",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text("Gallery", color = Color.White, fontSize = 12.sp)
                    }

                    // Capture button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(72.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val capture = imageCapture ?: return@IconButton
                                    capture.takePicture(
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(image: ImageProxy) {
                                                val bitmap = image.toBitmap()
                                                    .copy(Bitmap.Config.ARGB_8888, true)
                                                image.close()
                                                capturedBitmap = bitmap
                                                analyzeImage(bitmap)
                                            }

                                            override fun onError(exc: ImageCaptureException) {
                                                Log.e("ReceiptScanner", "Capture error: ${exc.message}")
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Capture failed: ${exc.message}")
                                                }
                                            }
                                        }
                                    )
                                },
                                enabled = cameraPermission.status.isGranted,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Capture",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text("Capture", color = Color.White, fontSize = 12.sp)
                    }

                    // Spacer to balance layout
                    Spacer(modifier = Modifier.size(56.dp))
                }
            }

            // Loading overlay while scanning / matching
            if (uiState is ReceiptScannerViewModel.ScannerUiState.Scanning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth(0.80f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val statusText = when {
                                analysisProgress >= 0.8f -> "Matching line items…"
                                analysisProgress >= 0.3f -> "Analyzing receipt…"
                                else                     -> "Scanning receipt…"
                            }
                            val subText = when {
                                departmentLineItems.isNotEmpty() -> "Matching against ${departmentLineItems.size} item(s)"
                                else                             -> "Extracting fields on-device"
                            }

                            if (analysisProgress > 0f && analysisProgress < 1f) {
                                LinearProgressIndicator(
                                    progress = { analysisProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = colorScheme.primary,
                                    trackColor = colorScheme.surfaceVariant
                                )
                            } else {
                                CircularProgressIndicator(
                                    color = colorScheme.primary,
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 3.dp
                                )
                            }

                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = subText,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Error state
            if (uiState is ReceiptScannerViewModel.ScannerUiState.Error) {
                val errorMsg = (uiState as ReceiptScannerViewModel.ScannerUiState.Error).message
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Scan Failed",
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onErrorContainer,
                                fontSize = 18.sp
                            )
                            Text(
                                text = errorMsg,
                                color = colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.reset()
                                        onDismiss()
                                    }
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = { viewModel.reset() },
                                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                                ) {
                                    Text("Retry", color = colorScheme.onError)
                                }
                            }
                        }
                    }
                }
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 160.dp)
            )
        }
    }
}

private fun saveBitmapToUri(context: android.content.Context, bitmap: Bitmap): Uri? {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DISPLAY_NAME, "receipt_scan_${System.currentTimeMillis()}.jpg")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
        }
        uri
    } catch (e: Exception) {
        Log.e("ReceiptScanner", "Failed to save bitmap: ${e.message}")
        null
    }
}
