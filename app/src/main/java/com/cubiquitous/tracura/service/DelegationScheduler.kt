package com.cubiquitous.tracura.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.cubiquitous.tracura.worker.DelegationExpiryWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DelegationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {
    
    companion object {
        private const val TAG = "DelegationScheduler"
        private const val WORK_NAME = DelegationExpiryWorker.WORK_NAME
    }
    
    /**
     * Schedule periodic delegation expiry checks
     * Runs every 6 hours to check for expired delegations
     */
    fun scheduleDelegationExpiryChecks() {
        try {
            Log.d(TAG, "🔄 Scheduling delegation expiry checks")
            
            // Create constraints for the work
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            // Create periodic work request
            val delegationExpiryWork = PeriodicWorkRequestBuilder<DelegationExpiryWorker>(
                6, // Repeat every 6 hours
                TimeUnit.HOURS,
                1, // Flex interval of 1 hour
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag("delegation_expiry")
                .build()
            
            // Enqueue the work with unique work name
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
                delegationExpiryWork
            )
            
            Log.d(TAG, "✅ Delegation expiry checks scheduled successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error scheduling delegation expiry checks: ${e.message}", e)
        }
    }
    
    /**
     * Cancel delegation expiry checks
     */
    fun cancelDelegationExpiryChecks() {
        try {
            Log.d(TAG, "🔄 Cancelling delegation expiry checks")
            workManager.cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "✅ Delegation expiry checks cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling delegation expiry checks: ${e.message}", e)
        }
    }
    
    /**
     * Run delegation expiry check immediately (for testing or manual triggers)
     */
    fun runDelegationExpiryCheckNow() {
        try {
            Log.d(TAG, "🔄 Running immediate delegation expiry check")
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val immediateWork = OneTimeWorkRequestBuilder<DelegationExpiryWorker>()
                .setConstraints(constraints)
                .addTag("delegation_expiry_immediate")
                .build()
            
            workManager.enqueue(immediateWork)
            
            Log.d(TAG, "✅ Immediate delegation expiry check enqueued")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error running immediate delegation expiry check: ${e.message}", e)
        }
    }
    
    /**
     * Check if delegation expiry checks are currently scheduled
     */
    fun isDelegationExpiryScheduled(): Boolean {
        return try {
            val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
            workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking delegation expiry schedule: ${e.message}", e)
            false
        }
    }
}
