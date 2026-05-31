package com.cubiquitous.tracura.viewmodel

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.Phase
import com.cubiquitous.tracura.repository.ProjectRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class PhaseCompletionViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var isUploading by mutableStateOf(false)
        private set

    var uploadProgress by mutableFloatStateOf(0f)
        private set

    /**
     * Complete a phase with an optional proof file upload.
     * 1. Resolves customerId
     * 2. Uploads proof to Firebase Storage (if provided)
     * 3. Updates Firestore phase document (endDate + phaseOverProofUrl)
     * 4. Writes a changelog entry
     */
    fun completePhaseWithProof(
        projectId: String,
        phase: Phase,
        imageUri: Uri? = null,
        fileUri: Uri? = null,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            isUploading = true
            uploadProgress = 0f
            try {
                // Resolve customerId first
                val customerId = projectRepository.findCustomerIdForProject(projectId)
                    ?: throw Exception("Could not find customer for project")

                val proofUrl: String? = when {
                    imageUri != null -> uploadProof(
                        customerId, projectId, phase.id, imageUri, context, isImage = true
                    )
                    fileUri != null -> uploadProof(
                        customerId, projectId, phase.id, fileUri, context, isImage = false
                    )
                    else -> null
                }

                completePhaseInFirestore(customerId, projectId, phase, proofUrl)

                isUploading = false
                onSuccess()
            } catch (e: Exception) {
                isUploading = false
                onError(e.message ?: "Upload failed")
            }
        }
    }

    private suspend fun uploadProof(
        customerId: String,
        projectId: String,
        phaseId: String,
        uri: Uri,
        context: Context,
        isImage: Boolean
    ): String {
        val timestamp = System.currentTimeMillis()
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?: if (isImage) "jpg" else "bin"
        val fileName = "phase_proof_${phaseId}_${timestamp}.$extension"

        val storageRef = storage.reference
            .child("customers/$customerId/projects/$projectId/phases/$phaseId/proofs/$fileName")

        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            ?: throw Exception("Cannot read file")

        return suspendCoroutine { continuation ->
            val uploadTask = storageRef.putBytes(bytes)
            uploadTask.addOnProgressListener { snapshot ->
                val total = snapshot.totalByteCount
                uploadProgress = if (total > 0) {
                    snapshot.bytesTransferred.toFloat() / total.toFloat()
                } else 0f
            }
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                storageRef.downloadUrl
            }.addOnSuccessListener { downloadUri ->
                continuation.resume(downloadUri.toString())
            }.addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
        }
    }

    private suspend fun completePhaseInFirestore(
        customerId: String,
        projectId: String,
        phase: Phase,
        proofUrl: String?
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.time
        val endDateStr = sdf.format(yesterday)

        val updateData = hashMapOf<String, Any>(
            "endDate" to endDateStr,
            "isEnabled" to false,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        proofUrl?.let { updateData["phaseOverProofUrl"] = it }

        val phaseRef = firestore
            .collection("customers").document(customerId)
            .collection("projects").document(projectId)
            .collection("phases").document(phase.id)

        phaseRef.update(updateData).await()

        // Write changelog entry
        val currentUser = auth.currentUser ?: return
        val changeLog = hashMapOf<String, Any?>(
            "phaseId" to phase.id,
            "projectId" to projectId,
            "previousStartDate" to phase.startDate,
            "previousEndDate" to phase.endDate,
            "newStartDate" to phase.startDate,
            "newEndDate" to endDateStr,
            "changedBy" to currentUser.uid,
            "requestID" to null,
            "timestamp" to FieldValue.serverTimestamp()
        )
        phaseRef.collection("changes").add(changeLog).await()
    }

    fun resetState() {
        isUploading = false
        uploadProgress = 0f
    }
}
