package com.cubiquitous.tracura.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility class for handling runtime permissions
 */
object PermissionUtils {
    
    // SMS permissions for OTP auto-fill
    val SMS_PERMISSIONS = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    )
    
    /**
     * Check if SMS permissions are granted
     */
    fun hasSMSPermissions(context: Context): Boolean {
        return SMS_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Request SMS permissions
     */
    fun requestSMSPermissions(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, SMS_PERMISSIONS, requestCode)
    }
    
    /**
     * Check if a specific permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if we should show rationale for permission
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}
