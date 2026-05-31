package com.cubiquitous.tracura

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.cubiquitous.tracura.service.TracuraFirebaseMessagingService
import com.cubiquitous.tracura.service.DelegationScheduler
import com.cubiquitous.tracura.service.PresenceManager
import com.cubiquitous.tracura.service.TemporaryApproverExpirationManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TracuraApplication : Application() {

    @Inject
    lateinit var delegationScheduler: DelegationScheduler

    @Inject
    lateinit var presenceManager: PresenceManager
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d("TracuraApplication", "🚀 Application starting...")

        // Register process-level lifecycle observer to manually set offline on background
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                presenceManager.goOffline()
            }
        })

        // Initialize Firebase Cloud Messaging
        initializeFCM()
        
        // Initialize temporary approver expiration service
        try {
            TemporaryApproverExpirationManager.initialize(this)
            Log.d("TracuraApplication", "✅ Temporary approver expiration service initialized")
        } catch (e: Exception) {
            Log.e("TracuraApplication", "❌ Failed to initialize expiration service", e)
        }
        
        // Initialize delegation scheduler
        try {
            delegationScheduler.scheduleDelegationExpiryChecks()
            Log.d("TracuraApplication", "✅ Delegation expiry scheduler initialized")
        } catch (e: Exception) {
            Log.e("TracuraApplication", "❌ Failed to initialize delegation scheduler", e)
        }
        
        // Initialize notification channels
        try {
            TracuraFirebaseMessagingService.createAllNotificationChannels(this)
            Log.d("TracuraApplication", "✅ Notification channels initialized")
        } catch (e: Exception) {
            Log.e("TracuraApplication", "❌ Failed to initialize notification channels", e)
        }
        
        Log.d("TracuraApplication", "✅ Application initialization completed")
    }
    
    /**
     * Initialize Firebase Cloud Messaging
     * This ensures FCM is ready to receive push notifications from the server
     */
    private fun initializeFCM() {
        try {
            val messaging = FirebaseMessaging.getInstance()
            
            // Subscribe to default topic (optional - for broadcast notifications)
            // messaging.subscribeToTopic("all_users")
            
            // Get FCM token to verify initialization
            messaging.token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("TracuraApplication", "✅ FCM initialized successfully")
                    Log.d("TracuraApplication", "📱 FCM Token: ${token?.take(20)}...")
                } else {
                    Log.e("TracuraApplication", "❌ Failed to get FCM token: ${task.exception?.message}")
                }
            }
            
            Log.d("TracuraApplication", "✅ FCM initialization started")
        } catch (e: Exception) {
            Log.e("TracuraApplication", "❌ Failed to initialize FCM: ${e.message}", e)
        }
    }
} 