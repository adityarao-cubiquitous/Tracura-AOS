package com.cubiquitous.tracura.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * OTP Manager to handle automatic OTP detection and state management
 * This singleton class manages the detected OTP and provides it to the UI
 */
object OTPManager {
    private const val TAG = "OTPManager"
    
    // State flow for detected OTP
    private val _detectedOTP = MutableStateFlow<String?>(null)
    val detectedOTP: StateFlow<String?> = _detectedOTP.asStateFlow()
    
    // State flow for OTP detection status
    private val _isOTPDetected = MutableStateFlow(false)
    val isOTPDetected: StateFlow<Boolean> = _isOTPDetected.asStateFlow()
    
    // State flow for OTP source (SMS, etc.)
    private val _otpSource = MutableStateFlow<String?>(null)
    val otpSource: StateFlow<String?> = _otpSource.asStateFlow()
    
    /**
     * Set the detected OTP from SMS or other sources
     */
    fun setDetectedOTP(otp: String, source: String = "SMS") {
        Log.d(TAG, "🔑 Setting detected OTP: $otp from source: $source")
        _detectedOTP.value = otp
        _isOTPDetected.value = true
        _otpSource.value = source
    }
    
    /**
     * Get the current detected OTP
     */
    fun getDetectedOTP(): String? {
        return _detectedOTP.value
    }
    
    /**
     * Clear the detected OTP (call this after successful verification)
     */
    fun clearDetectedOTP() {
        Log.d(TAG, "🗑️ Clearing detected OTP")
        _detectedOTP.value = null
        _isOTPDetected.value = false
        _otpSource.value = null
    }
    
    /**
     * Check if OTP is currently detected
     */
    fun hasDetectedOTP(): Boolean {
        return _isOTPDetected.value && _detectedOTP.value != null
    }
    
    /**
     * Get OTP detection status
     */
    fun isOTPDetected(): Boolean {
        return _isOTPDetected.value
    }
    
    /**
     * Reset all OTP state
     */
    fun reset() {
        Log.d(TAG, "🔄 Resetting OTP manager state")
        _detectedOTP.value = null
        _isOTPDetected.value = false
        _otpSource.value = null
    }
}
