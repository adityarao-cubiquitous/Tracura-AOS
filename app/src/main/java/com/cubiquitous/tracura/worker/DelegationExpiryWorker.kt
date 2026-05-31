package com.cubiquitous.tracura.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cubiquitous.tracura.service.DelegationExpiryService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DelegationExpiryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val delegationExpiryService: DelegationExpiryService
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "DelegationExpiryWorker"
        const val WORK_NAME = "delegation_expiry_check"
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "🚀 Starting delegation expiry worker")
            
            // Process expired delegations
            delegationExpiryService.processExpiredDelegations()
            
            Log.d(TAG, "✅ Delegation expiry worker completed successfully")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Delegation expiry worker failed: ${e.message}", e)
            Result.failure()
        }
    }
}
