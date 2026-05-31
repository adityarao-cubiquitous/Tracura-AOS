package com.cubiquitous.tracura.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.cubiquitous.tracura.service.OTPDetectionService
import com.cubiquitous.tracura.utils.OTPManager
import java.util.regex.Pattern

/**
 * SMS Receiver to automatically detect and extract OTP from incoming SMS messages
 * This receiver listens for SMS messages and extracts OTP codes for auto-filling
 */
class OTPSmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "OTPSmsReceiver"
        
        // Common OTP patterns for different services
        private val OTP_PATTERNS = listOf(
            // Firebase/Google OTP patterns
            Pattern.compile("(\\d{6})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("code[\\s:]+(\\d{6})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("verification[\\s:]+(\\d{6})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("otp[\\s:]+(\\d{6})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{6})[\\s]*is[\\s]*your", Pattern.CASE_INSENSITIVE),
            Pattern.compile("your[\\s]*code[\\s]*is[\\s]*(\\d{6})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{6})[\\s]*verification", Pattern.CASE_INSENSITIVE),
            
            // Generic 6-digit OTP patterns
            Pattern.compile("(\\d{6})", Pattern.CASE_INSENSITIVE),
            
            // 4-digit OTP patterns (less common but some services use them)
            Pattern.compile("(\\d{4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("code[\\s:]+(\\d{4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("verification[\\s:]+(\\d{4})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("otp[\\s:]+(\\d{4})", Pattern.CASE_INSENSITIVE)
        )
        
        // Keywords that indicate this is an OTP message
        private val OTP_KEYWORDS = listOf(
            "otp", "verification", "code", "pin", "password", "firebase", "google",
            "verify", "authentication", "login", "signin", "confirm"
        )
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📱 SMS received, checking for OTP...")
        
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (message in messages) {
                val messageBody = message.messageBody ?: ""
                val sender = message.originatingAddress ?: ""
                
                Log.d(TAG, "📨 SMS from: $sender")
                Log.d(TAG, "📨 SMS body: $messageBody")
                
                // Check if this message contains an OTP
                val otp = extractOTP(messageBody)
                if (otp != null) {
                    Log.d(TAG, "✅ OTP detected: $otp from sender: $sender")
                    
                    // Use OTP detection service for consistent handling
                    val otpDetectionService = OTPDetectionService(context)
                    otpDetectionService.handleSMSPermissionMessage(messageBody)
                } else {
                    Log.d(TAG, "❌ No OTP found in message from: $sender")
                }
            }
        }
    }
    
    /**
     * Extract OTP from SMS message body using various patterns
     */
    private fun extractOTP(messageBody: String): String? {
        // First check if the message contains OTP-related keywords
        val containsOTPKeywords = OTP_KEYWORDS.any { keyword ->
            messageBody.lowercase().contains(keyword.lowercase())
        }
        
        if (!containsOTPKeywords) {
            Log.d(TAG, "🔍 Message doesn't contain OTP keywords, skipping...")
            return null
        }
        
        // Try to extract OTP using different patterns
        for (pattern in OTP_PATTERNS) {
            val matcher = pattern.matcher(messageBody)
            if (matcher.find()) {
                val otp = matcher.group(1)
                if (otp != null && otp.length in 4..6) {
                    Log.d(TAG, "✅ OTP extracted using pattern: $pattern")
                    return otp
                }
            }
        }
        
        // Fallback: look for any sequence of 4-6 digits
        val fallbackPattern = Pattern.compile("\\b(\\d{4,6})\\b")
        val matcher = fallbackPattern.matcher(messageBody)
        if (matcher.find()) {
            val otp = matcher.group(1)
            Log.d(TAG, "✅ OTP extracted using fallback pattern: $otp")
            return otp
        }
        
        return null
    }
}
