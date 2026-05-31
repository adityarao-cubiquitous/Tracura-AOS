package com.cubiquitous.tracura

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.rememberNavController
import com.cubiquitous.tracura.navigation.AppNavHost
import com.cubiquitous.tracura.service.TracuraFirebaseMessagingService
import com.cubiquitous.tracura.ui.theme.AvrTheme
import com.cubiquitous.tracura.utils.NotificationLocalStorageManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // Holds the current notification intent so Compose can observe it
    private val notificationIntent = mutableStateOf<Intent?>(null)
    private val localStorageManager by lazy { NotificationLocalStorageManager(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize all notification channels
        TracuraFirebaseMessagingService.createAllNotificationChannels(this)
        
        // Request notification permission for Android 13+
        requestNotificationPermission()

        handleNotificationTapIntent(intent)
        
        // Capture initial notification intent (cold start).
        // fromNotification can be a Boolean (set by showNotification()) or a String "true"
        // (set in FCM data payload and delivered as a String extra by Android for tray notifications).
        if (isNotificationIntent(intent)) {
            notificationIntent.value = intent
        }
        
        enableEdgeToEdge()
        setContent {
            AvrTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    notificationIntent = notificationIntent.value,
                    onNotificationConsumed = {
                        // Clear the intent so it's not re-processed on recomposition
                        notificationIntent.value = null
                    }
                )
            }
        }
    }
    
    // Called when app is already running and user taps a notification (warm start)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationTapIntent(intent)
        if (isNotificationIntent(intent)) {
            notificationIntent.value = intent
        }
    }

    private fun handleNotificationTapIntent(intent: Intent?) {
        val tappedMessageId = intent?.extras
            ?.get("google.message_id")
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: return

        localStorageManager.deleteNotificationByMessageId(tappedMessageId)
        android.util.Log.d(
            "MainActivity",
            "🗑️ Deleted tapped notification from local storage using google.message_id: $tappedMessageId"
        )
    }

    /**
     * Returns true if the intent originated from a tapped notification.
     *
     * Handles three cases:
     *  1. Boolean extra set by showNotification() when app is in foreground.
     *  2. String "true" extra – FCM data payload extras are delivered as Strings when
     *     Android auto-handles a background/killed notification payload.
     *  3. A projectId is present (any notification we care about carries one).
     */
    private fun isNotificationIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        return intent.getBooleanExtra("fromNotification", false) ||
               intent.getStringExtra("fromNotification") == "true" ||
             intent.getStringExtra("projectId")?.isNotEmpty() == true ||
             intent.getStringExtra("google.message_id")?.isNotEmpty() == true
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }
}