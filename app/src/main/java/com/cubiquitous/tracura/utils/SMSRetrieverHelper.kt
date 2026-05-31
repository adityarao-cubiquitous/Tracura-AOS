package com.cubiquitous.tracura.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.tasks.Task

/**
 * Helper class for Google SMS Retriever API
 * This API doesn't require SMS permissions and works through Google Play Services
 */
object SMSRetrieverHelper {
    private const val TAG = "SMSRetrieverHelper"
    
    /**
     * Start listening for SMS messages using Google SMS Retriever API
     * This method doesn't require SMS permissions
     */
    suspend fun startSMSRetriever(context: Context): Task<Void> {
        Log.d(TAG, "🔄 Starting SMS Retriever...")
        return SmsRetriever.getClient(context).startSmsRetriever()
    }
    
    /**
     * Stop listening for SMS messages
     * Note: SMS Retriever automatically stops after timeout or successful retrieval
     */
    fun stopSMSRetriever(context: Context) {
        Log.d(TAG, "🛑 SMS Retriever will stop automatically after timeout or successful retrieval")
        // SMS Retriever doesn't have a manual stop method
        // It automatically stops after 5 minutes or when SMS is retrieved
    }
    
    /**
     * Start SMS Retriever and return the task
     * This method starts the SMS Retriever and returns immediately
     * The actual SMS will be received via SMSRetrieverReceiver
     */
    fun startSMSRetrieverSync(context: Context): Task<Void> {
        Log.d(TAG, "🔄 Starting SMS Retriever synchronously...")
        return SmsRetriever.getClient(context).startSmsRetriever()
    }
    
    /**
     * Parse SMS message and extract OTP
     * This method handles the SMS message format from Google SMS Retriever
     */
    fun parseSMSMessage(smsMessage: String): String? {
        Log.d(TAG, "📨 Parsing SMS message: $smsMessage")
        
        // Google SMS Retriever typically sends messages in a specific format
        // Look for OTP patterns in the message
        val otpPatterns = listOf(
            Regex("(\\d{6})"), // 6-digit OTP
            Regex("(\\d{4})"), // 4-digit OTP
            Regex("code[\\s:]+(\\d{6})", RegexOption.IGNORE_CASE),
            Regex("verification[\\s:]+(\\d{6})", RegexOption.IGNORE_CASE),
            Regex("otp[\\s:]+(\\d{6})", RegexOption.IGNORE_CASE),
            Regex("(\\d{6})[\\s]*is[\\s]*your", RegexOption.IGNORE_CASE),
            Regex("your[\\s]*code[\\s]*is[\\s]*(\\d{6})", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in otpPatterns) {
            val match = pattern.find(smsMessage)
            if (match != null) {
                val otp = match.groupValues[1]
                Log.d(TAG, "✅ OTP extracted: $otp")
                return otp
            }
        }
        
        Log.d(TAG, "❌ No OTP found in SMS message")
        return null
    }
    
    /**
     * Check if Google Play Services is available
     */
    fun isGooglePlayServicesAvailable(context: Context): Boolean {
        return try {
            SmsRetriever.getClient(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Play Services not available", e)
            false
        }
    }
}
