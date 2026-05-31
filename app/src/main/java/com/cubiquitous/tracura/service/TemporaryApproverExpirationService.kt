package com.cubiquitous.tracura.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.cubiquitous.tracura.repository.TemporaryApproverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Background service to automatically check and deactivate expired temporary approvers
 */
class TemporaryApproverExpirationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "TempApproverExpiration"
        const val WORK_NAME = "temporary_approver_expiration_check"
        
        /**
         * Schedule periodic expiration checks
         */
        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val periodicWorkRequest = PeriodicWorkRequestBuilder<TemporaryApproverExpirationWorker>(
                repeatInterval = 6, // Check every 6 hours
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    periodicWorkRequest
                )
            
            Log.d(TAG, "✅ Scheduled periodic temporary approver expiration checks")
        }
        
        /**
         * Schedule an immediate one-time check
         */
        fun scheduleImmediateCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<TemporaryApproverExpirationWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context)
                .enqueue(oneTimeWorkRequest)
            
            Log.d(TAG, "✅ Scheduled immediate temporary approver expiration check")
        }
        
        /**
         * Cancel all scheduled expiration checks
         */
        fun cancelExpirationChecks(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME)
            
            Log.d(TAG, "✅ Cancelled temporary approver expiration checks")
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 Starting temporary approver expiration check...")
            
            // Create repository instance
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            // Create FCM service for NotificationRepository
            // NotificationRepository no longer requires FCMPushNotificationService
            // Push notifications are handled by Cloud Functions based on Firestore changes
            val notificationRepository = com.cubiquitous.tracura.repository.NotificationRepository(firestore)
            val authRepository = com.cubiquitous.tracura.repository.AuthRepository(firestore)
            
            // Create repositories in the correct order to avoid circular dependency
            // First create a dummy Lazy wrapper for NotificationService
            val dummyLazyNotificationService: dagger.Lazy<com.cubiquitous.tracura.service.NotificationService> = 
                object : dagger.Lazy<com.cubiquitous.tracura.service.NotificationService> {
                    override fun get(): com.cubiquitous.tracura.service.NotificationService {
                        throw IllegalStateException("NotificationService should not be accessed in TemporaryApproverExpirationService")
                    }
                }
            val temporaryApproverRepository = TemporaryApproverRepository(firestore, dummyLazyNotificationService)
            val projectRepository = com.cubiquitous.tracura.repository.ProjectRepository(
                firestore,
                temporaryApproverRepository,
                authRepository,
                applicationContext,
            )
            
            // Get all projects
            val allProjects = getAllProjects(temporaryApproverRepository)
            Log.d(TAG, "📋 Found ${allProjects.size} projects to check")
            
            var totalDeactivated = 0
            var projectsChecked = 0
            
            // Check each project for expired temporary approvers
            for (project in allProjects) {
                try {
                    val projectId = project.id ?: continue
                    val deactivatedCount = temporaryApproverRepository.deactivateExpiredApprovers(projectId)
                    
                    if (deactivatedCount.isSuccess) {
                        val count = deactivatedCount.getOrNull() ?: 0
                        totalDeactivated += count
                        projectsChecked++
                        
                        if (count > 0) {
                            Log.d(TAG, "✅ Deactivated $count expired temporary approvers in project: ${project.name}")
                        }
                    } else {
                        Log.w(TAG, "⚠️ Failed to check project ${project.name}: ${deactivatedCount.exceptionOrNull()?.message}")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error checking project ${project.name}", e)
                }
            }
            
            Log.d(TAG, "✅ Expiration check completed - $totalDeactivated approvers deactivated across $projectsChecked projects")
            
            // If we deactivated any approvers, we could send notifications to production heads
            if (totalDeactivated > 0) {
                Log.d(TAG, "📧 Consider sending notification about $totalDeactivated expired temporary approvers")
                // Notify production heads about expired approvers
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Temporary approver expiration check failed", e)
            Result.failure()
        }
    }
    
    /**
     * Get all projects from the repository
     */
    private suspend fun getAllProjects(temporaryApproverRepository: TemporaryApproverRepository): List<com.cubiquitous.tracura.model.Project> {
        return try {
            // Since ProjectRepository doesn't have a simple getAllProjects method,
            // we'll query Firestore directly
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("Project1").get().await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val projectData = doc.data ?: return@mapNotNull null
                    
                    // Manually parse with correct types
                    val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val startDateString = when (val startDate = projectData["startDate"]) {
                        is com.google.firebase.Timestamp -> dateFormatter.format(startDate.toDate())
                        is String -> startDate
                        else -> null
                    }
                    val endDateString = when (val endDate = projectData["endDate"]) {
                        is com.google.firebase.Timestamp -> dateFormatter.format(endDate.toDate())
                        is String -> endDate
                        else -> null
                    }
                    val createdAt = when (val created = projectData["createdAt"]) {
                        is com.google.firebase.Timestamp -> created
                        else -> com.google.firebase.Timestamp.now()
                    }
                    val updatedAt = when (val updated = projectData["updatedAt"]) {
                        is com.google.firebase.Timestamp -> updated
                        else -> com.google.firebase.Timestamp.now()
                    }
                    com.cubiquitous.tracura.model.Project(
                        id = doc.id,
                        name = projectData["name"] as? String ?: "",
                        description = projectData["description"] as? String ?: "",
                        budget = (projectData["budget"] as? Number)?.toDouble() ?: 0.0,
                        status = projectData["status"] as? String ?: "INACTIVE",
                        startDate = startDateString,
                        endDate = endDateString,
                        teamMembers = (projectData["teamMembers"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        tempApproverID = projectData["tempApproverID"] as? String,
                        Allow_Template_Overrides = projectData["Allow_Template_Overrides"] as? Boolean,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        spent = (projectData["spent"] as? Number)?.toDouble(),
                        managerId = projectData["managerId"] as? String,
                        approverIds = (projectData["approverIds"] as? List<*>)?.mapNotNull { it as? String },
                        BusinessHeadIds = (projectData["BusinessHeadIds"] as? List<*>)?.mapNotNull { it as? String },
                        code = projectData["code"] as? String,
                        departmentBudgets = (projectData["departmentBudgets"] as? Map<String, Any>)?.mapValues { 
                            (it.value as? Number)?.toDouble() ?: 0.0 
                        },
                        categories = (projectData["categories"] as? List<*>)?.mapNotNull { it as? String }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing project document ${doc.id}", e)
                    null
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting all projects", e)
            emptyList()
        }
    }
}

/**
 * Utility class to manage temporary approver expiration service
 */
object TemporaryApproverExpirationManager {
    
    private const val TAG = "TempApproverExpirationMgr"
    
    /**
     * Initialize the expiration service - call this when app starts
     */
    fun initialize(context: Context) {
        try {
            Log.d(TAG, "🔄 Initializing temporary approver expiration service...")
            
            // Schedule periodic checks
            TemporaryApproverExpirationWorker.schedulePeriodicCheck(context)
            
            // Schedule an immediate check
            TemporaryApproverExpirationWorker.scheduleImmediateCheck(context)
            
            Log.d(TAG, "✅ Temporary approver expiration service initialized")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize temporary approver expiration service", e)
        }
    }
    
    /**
     * Manually trigger an expiration check
     */
    fun triggerExpirationCheck(context: Context) {
        Log.d(TAG, "🔄 Manually triggering expiration check...")
        TemporaryApproverExpirationWorker.scheduleImmediateCheck(context)
    }
    
    /**
     * Stop the expiration service
     */
    fun shutdown(context: Context) {
        Log.d(TAG, "🔄 Shutting down temporary approver expiration service...")
        TemporaryApproverExpirationWorker.cancelExpirationChecks(context)
        Log.d(TAG, "✅ Temporary approver expiration service shutdown")
    }
}
