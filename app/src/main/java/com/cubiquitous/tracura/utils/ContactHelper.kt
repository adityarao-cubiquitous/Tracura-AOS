package com.cubiquitous.tracura.utils

import android.content.Context
import android.provider.ContactsContract
import android.Manifest
import androidx.core.content.ContextCompat

/**
 * Utility class for checking if a phone number exists in device contacts
 */
object ContactHelper {
    
    /**
     * Check if a phone number exists in device contacts
     * @param context The application context
     * @param phoneNumber The phone number to check (should be digits only)
     * @return true if the phone number exists in contacts, false otherwise
     */
    fun isPhoneNumberInContacts(context: Context, phoneNumber: String): Boolean {
        // Check if READ_CONTACTS permission is granted
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        
        // Normalize phone number (remove non-digits)
        val normalizedPhone = phoneNumber.filter { it.isDigit() }
        if (normalizedPhone.isEmpty()) {
            return false
        }
        
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val selectionArgs = arrayOf("%$normalizedPhone%")
        
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val contactNumber = cursor.getString(0) ?: continue
                // Normalize contact number and compare
                val normalizedContactNumber = contactNumber.filter { it.isDigit() }
                
                // Check if the normalized phone number matches or is contained in contact number
                if (normalizedContactNumber.contains(normalizedPhone) || 
                    normalizedPhone.contains(normalizedContactNumber)) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Get contact name for a phone number if it exists
     * @param context The application context
     * @param phoneNumber The phone number to check
     * @return The contact name if found, null otherwise
     */
    fun getContactName(context: Context, phoneNumber: String): String? {
        // Check if READ_CONTACTS permission is granted
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        
        // Normalize phone number (remove non-digits)
        val normalizedPhone = phoneNumber.filter { it.isDigit() }
        if (normalizedPhone.isEmpty()) {
            return null
        }
        
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val selectionArgs = arrayOf("%$normalizedPhone%")
        
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val contactName = cursor.getString(0)
                val contactNumber = cursor.getString(1) ?: continue
                
                // Normalize contact number and compare
                val normalizedContactNumber = contactNumber.filter { it.isDigit() }
                
                // Check if the normalized phone number matches or is contained in contact number
                if (normalizedContactNumber.contains(normalizedPhone) || 
                    normalizedPhone.contains(normalizedContactNumber)) {
                    return contactName
                }
            }
        }
        
        return null
    }
}

