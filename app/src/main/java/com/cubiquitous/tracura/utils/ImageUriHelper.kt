package com.cubiquitous.tracura.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.InputStream

object ImageUriHelper {
    
    /**
     * Creates a proper FileProvider URI for camera images
     */
    fun createCameraImageUri(context: Context, fileName: String): Uri? {
        return try {
            // Use cache directory as primary method (more reliable for FileProvider)
            val tempFile = File(context.cacheDir, fileName)
            
            android.util.Log.d("ImageUriHelper", "Creating camera image URI for file: ${tempFile.absolutePath}")
            android.util.Log.d("ImageUriHelper", "Cache directory: ${context.cacheDir.absolutePath}")
            android.util.Log.d("ImageUriHelper", "Cache directory exists: ${context.cacheDir.exists()}")
            android.util.Log.d("ImageUriHelper", "Cache directory can write: ${context.cacheDir.canWrite()}")
            
            // Ensure the file is created
            if (!tempFile.exists()) {
                val created = tempFile.createNewFile()
                android.util.Log.d("ImageUriHelper", "File created: $created")
            }
            
            android.util.Log.d("ImageUriHelper", "File exists: ${tempFile.exists()}")
            android.util.Log.d("ImageUriHelper", "File can read: ${tempFile.canRead()}")
            android.util.Log.d("ImageUriHelper", "File can write: ${tempFile.canWrite()}")
            
            // Create FileProvider URI
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            
            android.util.Log.d("ImageUriHelper", "FileProvider URI created: $uri")
            android.util.Log.d("ImageUriHelper", "URI scheme: ${uri.scheme}")
            android.util.Log.d("ImageUriHelper", "URI path: ${uri.path}")
            
            uri
        } catch (e: Exception) {
            android.util.Log.e("ImageUriHelper", "Error creating camera image URI: ${e.message}", e)
            android.util.Log.e("ImageUriHelper", "Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("ImageUriHelper", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            null
        }
    }
    
    /**
     * Tests if a URI is accessible for reading
     */
    fun isUriAccessible(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val accessible = inputStream != null
            inputStream?.close()
            accessible
        } catch (e: Exception) {
            android.util.Log.e("ImageUriHelper", "Error testing URI accessibility: ${e.message}", e)
            false
        }
    }
    
    /**
     * Gets a readable file from URI if possible
     */
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            when (uri.scheme) {
                "file" -> File(uri.path ?: "")
                "content" -> {
                    // For content URIs, we can't get the file directly
                    // but we can test if it's accessible
                    if (isUriAccessible(context, uri)) {
                        // Create a temporary file and copy the content
                        val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            tempFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        tempFile
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageUriHelper", "Error getting file from URI: ${e.message}", e)
            null
        }
    }
    
    /**
     * Opens a file from Firebase Storage URL by downloading it first and then opening it
     */
    fun openFirebaseStorageFile(
        context: Context,
        storageUrl: String,
        fileName: String? = null,
        onError: ((String) -> Unit)? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("ImageUriHelper", "=== Starting to open Firebase Storage file ===")
                android.util.Log.d("ImageUriHelper", "Storage URL: $storageUrl")
                android.util.Log.d("ImageUriHelper", "File name: $fileName")
                android.util.Log.d("ImageUriHelper", "URL length: ${storageUrl.length}")
                android.util.Log.d("ImageUriHelper", "URL starts with https: ${storageUrl.startsWith("https://")}")
                android.util.Log.d("ImageUriHelper", "URL starts with gs: ${storageUrl.startsWith("gs://")}")
                
                // Determine file extension from URL or fileName
                val fileExtension = fileName?.substringAfterLast(".", "")
                    ?: storageUrl.substringAfterLast(".", "").substringBefore("?")
                
                // Create temporary file in cache directory
                val tempFileName = fileName ?: "attachment_${System.currentTimeMillis()}.$fileExtension"
                val tempFile = File(context.cacheDir, tempFileName)
                
                // Download file from Firebase Storage
                val storage = FirebaseStorage.getInstance()
                
                // Check what type of URL we have
                val isDownloadUrl = storageUrl.startsWith("https://firebasestorage.googleapis.com/")
                val isGsPath = storageUrl.startsWith("gs://")
                val isLocalUri = storageUrl.startsWith("content://") || storageUrl.startsWith("file://")
                
                android.util.Log.d("ImageUriHelper", "Is download URL: $isDownloadUrl")
                android.util.Log.d("ImageUriHelper", "Is GS path: $isGsPath")
                android.util.Log.d("ImageUriHelper", "Is local URI: $isLocalUri")
                
                if (isLocalUri) {
                    // It's a local URI (content:// or file://), copy directly
                    android.util.Log.d("ImageUriHelper", "Handling local URI: $storageUrl")
                    val sourceUri = Uri.parse(storageUrl)
                    
                    when (sourceUri.scheme) {
                        "file" -> {
                            // File URI - copy directly
                            val sourceFile = File(sourceUri.path ?: "")
                            if (sourceFile.exists()) {
                                sourceFile.copyTo(tempFile, overwrite = true)
                                android.util.Log.d("ImageUriHelper", "Copied file from: ${sourceFile.absolutePath}")
                            } else {
                                throw Exception("Source file does not exist: ${sourceFile.absolutePath}")
                            }
                        }
                        "content" -> {
                            // Content URI - read from content resolver with proper permissions
                            try {
                                android.util.Log.d("ImageUriHelper", "Attempting to read content URI: $sourceUri")
                                
                                // Try to take persistable URI permission first (for long-term access)
                                try {
                                    context.contentResolver.takePersistableUriPermission(
                                        sourceUri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                    android.util.Log.d("ImageUriHelper", "Successfully took persistable URI permission")
                                } catch (e: Exception) {
                                    android.util.Log.w("ImageUriHelper", "Could not take persistable permission (this is OK for some URIs): ${e.message}")
                                    // This is expected for some content URIs - continue anyway
                                }
                                
                                // Open input stream with read permission flag
                                val inputStream = try {
                                    context.contentResolver.openInputStream(sourceUri)
                                } catch (e: SecurityException) {
                                    android.util.Log.e("ImageUriHelper", "SecurityException when opening input stream: ${e.message}")
                                    // Try with explicit permission grant
                                    try {
                                        context.grantUriPermission(
                                            context.packageName,
                                            sourceUri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                        context.contentResolver.openInputStream(sourceUri)
                                    } catch (e2: Exception) {
                                        android.util.Log.e("ImageUriHelper", "Still cannot access after granting permission: ${e2.message}")
                                        throw Exception("Permission denied: Cannot access the attachment file. The file may have been moved or deleted. Original error: ${e.message}")
                                    }
                                }
                                
                                inputStream?.use { stream ->
                                    tempFile.outputStream().use { outputStream ->
                                        stream.copyTo(outputStream)
                                    }
                                    android.util.Log.d("ImageUriHelper", "Successfully copied content from URI to: ${tempFile.absolutePath}")
                                } ?: throw Exception("Cannot read from content URI: $storageUrl - InputStream is null. The file may have been deleted or moved.")
                                
                            } catch (e: SecurityException) {
                                android.util.Log.e("ImageUriHelper", "SecurityException reading content URI: ${e.message}", e)
                                throw Exception("Permission denied: Cannot access the attachment file. ${e.message}")
                            } catch (e: Exception) {
                                android.util.Log.e("ImageUriHelper", "Error reading content URI: ${e.message}", e)
                                throw Exception("Cannot read from content URI: ${e.message}")
                            }
                        }
                        else -> {
                            throw Exception("Unsupported URI scheme: ${sourceUri.scheme}")
                        }
                    }
                } else if (isDownloadUrl) {
                    // It's a download URL, download directly using HTTP
                    android.util.Log.d("ImageUriHelper", "Using direct HTTP download for: $storageUrl")
                    downloadFileFromUrl(storageUrl, tempFile)
                } else if (isGsPath) {
                    // It's a gs:// path, convert to storage reference
                    android.util.Log.d("ImageUriHelper", "Converting gs:// path to storage reference")
                    val path = storageUrl.removePrefix("gs://")
                    val bucketName = path.substringBefore("/")
                    val filePath = path.substringAfter("/")
                    android.util.Log.d("ImageUriHelper", "Bucket: $bucketName, Path: $filePath")
                    val storageReference = storage.getReference(filePath)
                    android.util.Log.d("ImageUriHelper", "Downloading file to: ${tempFile.absolutePath}")
                    storageReference.getFile(tempFile).await()
                } else {
                    // It's a storage path, use Firebase Storage reference
                    android.util.Log.d("ImageUriHelper", "Using storage path: $storageUrl")
                    val storageReference = storage.getReference(storageUrl)
                    android.util.Log.d("ImageUriHelper", "Downloading file to: ${tempFile.absolutePath}")
                    storageReference.getFile(tempFile).await()
                }
                
                android.util.Log.d("ImageUriHelper", "File downloaded successfully: ${tempFile.exists()}, size: ${tempFile.length()}")
                
                // Switch to main thread to open the file
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        // Get MIME type
                        val mimeType = getMimeType(fileExtension) ?: "*/*"
                        android.util.Log.d("ImageUriHelper", "MIME type: $mimeType")
                        
                        // Create FileProvider URI
                        val fileUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )
                        
                        android.util.Log.d("ImageUriHelper", "FileProvider URI: $fileUri")
                        
                        // Create intent to view the file
                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(fileUri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        
                        // Check if there's an app that can handle this file type
                        val packageManager = context.packageManager
                        val activities = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            packageManager.queryIntentActivities(
                                viewIntent,
                                android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_DEFAULT_ONLY.toLong())
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            packageManager.queryIntentActivities(viewIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                        }
                        
                        android.util.Log.d("ImageUriHelper", "Found ${activities.size} activities that can handle $mimeType")
                        
                        if (activities.isNotEmpty()) {
                            // Use chooser to let user select app
                            val chooserIntent = Intent.createChooser(viewIntent, "Open with")
                            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            android.util.Log.d("ImageUriHelper", "Starting chooser to view file")
                            context.startActivity(chooserIntent)
                            android.util.Log.d("ImageUriHelper", "File opened successfully")
                            onSuccess?.invoke()
                        } else {
                            android.util.Log.w("ImageUriHelper", "No app found to handle MIME type: $mimeType")
                            // Fallback 1: Try with generic mime type using chooser
                            android.util.Log.d("ImageUriHelper", "Trying fallback with generic mime type")
                            val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(fileUri, "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            
                            val genericActivities = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                packageManager.queryIntentActivities(
                                    genericIntent,
                                    android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_DEFAULT_ONLY.toLong())
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                packageManager.queryIntentActivities(genericIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                            }
                            
                            if (genericActivities.isNotEmpty()) {
                                val chooserIntent = Intent.createChooser(genericIntent, "Open with")
                                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                android.util.Log.d("ImageUriHelper", "Opening with generic mime type chooser")
                                context.startActivity(chooserIntent)
                                onSuccess?.invoke()
                            } else {
                                // Fallback 2: Open Firebase Storage URL in browser
                                android.util.Log.d("ImageUriHelper", "Trying to open Firebase Storage URL in browser")
                                if (storageUrl.startsWith("https://")) {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(storageUrl))
                                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(browserIntent)
                                        android.util.Log.d("ImageUriHelper", "Opened URL in browser")
                                        onSuccess?.invoke()
                                    } catch (ex: Exception) {
                                        android.util.Log.e("ImageUriHelper", "Failed to open URL in browser: ${ex.message}")
                                        onError?.invoke("No app found to open $mimeType files. Please install a PDF viewer or file manager app from Google Play Store.")
                                    }
                                } else {
                                    android.util.Log.e("ImageUriHelper", "No fallback available for non-HTTPS URL")
                                    onError?.invoke("No app found to open $mimeType files. Please install a PDF viewer or file manager app from Google Play Store.")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ImageUriHelper", "Error opening file: ${e.message}", e)
                        // Final fallback: try opening URL directly
                        if (storageUrl.startsWith("https://")) {
                            try {
                                android.util.Log.d("ImageUriHelper", "Final fallback: Opening URL directly")
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(storageUrl))
                                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(browserIntent)
                                onSuccess?.invoke()
                            } catch (ex: Exception) {
                                android.util.Log.e("ImageUriHelper", "Final fallback also failed: ${ex.message}")
                                onError?.invoke("Failed to open file. Please install a PDF viewer or file manager app.")
                            }
                        } else {
                            onError?.invoke("Failed to open file: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ImageUriHelper", "Error downloading/accessing file: ${e.message}", e)
                android.util.Log.e("ImageUriHelper", "Exception type: ${e.javaClass.simpleName}")
                android.util.Log.e("ImageUriHelper", "Storage URL was: $storageUrl")
                e.printStackTrace()
                
                CoroutineScope(Dispatchers.Main).launch {
                    // Check if it's a permission error with local URI
                    val isPermissionError = e.message?.contains("permission", ignoreCase = true) == true ||
                            e.message?.contains("Permission denied", ignoreCase = true) == true ||
                            e is SecurityException
                    
                    val isLocalUri = storageUrl.startsWith("content://") || storageUrl.startsWith("file://")
                    
                    // Provide more specific error messages
                    val errorMessage = when {
                        isPermissionError && isLocalUri -> {
                            "⚠️ Cannot access attachment: The file was stored as a local reference which is no longer accessible. " +
                            "Please ask the user to re-upload the attachment or contact support."
                        }
                        e.message?.contains("doesn't exist", ignoreCase = true) == true -> {
                            "File not found. The attachment may have been deleted or moved."
                        }
                        isPermissionError -> {
                            "Permission denied. Cannot access the attachment file. Please check app permissions."
                        }
                        e.message?.contains("network", ignoreCase = true) == true -> {
                            "Network error. Please check your internet connection."
                        }
                        isLocalUri -> {
                            "Cannot access local file. The file may have been moved, deleted, or access has expired."
                        }
                        else -> {
                            "Failed to open attachment: ${e.message ?: "Unknown error"}"
                        }
                    }
                    
                    // Try fallback: open URL directly in browser if it's a web URL
                    if (storageUrl.startsWith("http://") || storageUrl.startsWith("https://")) {
                        try {
                            android.util.Log.d("ImageUriHelper", "Attempting fallback: opening URL in browser")
                            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(storageUrl)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                                android.util.Log.d("ImageUriHelper", "Opening URL in browser as fallback")
                                context.startActivity(fallbackIntent)
                                onSuccess?.invoke() // Consider browser opening as success
                                return@launch
                            } else {
                                android.util.Log.w("ImageUriHelper", "No browser app found for fallback")
                            }
                        } catch (ex: Exception) {
                            android.util.Log.e("ImageUriHelper", "Fallback also failed: ${ex.message}")
                        }
                    }
                    
                    onError?.invoke(errorMessage)
                }
            }
        }
    }
    
    /**
     * Downloads a file directly from a URL using HTTP
     */
    private suspend fun downloadFileFromUrl(url: String, destinationFile: File) {
        try {
            val connection = java.net.URL(url).openConnection()
            connection.connect()
            
            connection.getInputStream().use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            android.util.Log.d("ImageUriHelper", "File downloaded via HTTP: ${destinationFile.exists()}, size: ${destinationFile.length()}")
        } catch (e: Exception) {
            android.util.Log.e("ImageUriHelper", "Error downloading file from URL: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Gets MIME type from file extension
     */
    private fun getMimeType(extension: String): String? {
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            else -> null
        }
    }
}
