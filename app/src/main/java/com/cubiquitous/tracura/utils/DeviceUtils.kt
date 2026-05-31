package com.cubiquitous.tracura.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.cubiquitous.tracura.model.DeviceInfo
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object DeviceUtils {
    
    /**
     * Collect device information for the current device
     */
    suspend fun collectDeviceInfo(context: Context): DeviceInfo {
        return try {
            // Get FCM token
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            
            DeviceInfo(
                fcmToken = fcmToken,
                deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID),
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                osVersion = "Android ${Build.VERSION.RELEASE}",
                appVersion = "1.0.0", // You can get this from BuildConfig.VERSION_NAME
                lastLoginAt = System.currentTimeMillis(),
                isOnline = true
            )
        } catch (e: Exception) {
            // Return default device info if FCM token cannot be obtained
            DeviceInfo(
                fcmToken = "",
                deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID),
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                osVersion = "Android ${Build.VERSION.RELEASE}",
                appVersion = "1.0.0",
                lastLoginAt = System.currentTimeMillis(),
                isOnline = true
            )
        }
    }
    
    /**
     * Get device identifier
     */
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    
    /**
     * Get device model information
     */
    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    
    /**
     * Get Android version
     */
    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE}"
    }
} 