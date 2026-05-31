package com.cubiquitous.tracura.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cubiquitous.tracura.service.OTPDetectionService
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/**
 * BroadcastReceiver for Google SMS Retriever API
 * This receiver handles SMS messages detected by Google Play Services
 * No SMS permissions required
 */
class SMSRetrieverReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SMSRetrieverReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📱 SMS Retriever broadcast received")
        
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            val extras = intent.extras
            val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status
            
            when (status?.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    // SMS was retrieved successfully
                    val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                    if (message != null) {
                        Log.d(TAG, "✅ SMS retrieved successfully: $message")
                        
                        // Handle the SMS message through OTP detection service
                        val otpDetectionService = OTPDetectionService(context)
                        otpDetectionService.handleSMSRetrieverMessage(message)
                    } else {
                        Log.w(TAG, "⚠️ SMS message is null")
                    }
                }
                CommonStatusCodes.TIMEOUT -> {
                    Log.d(TAG, "⏰ SMS retrieval timeout")
                }
                else -> {
                    Log.e(TAG, "❌ SMS retrieval failed with status: ${status?.statusCode}")
                }
            }
        }
    }
}
