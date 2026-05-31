package com.cubiquitous.tracura.service

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceManager @Inject constructor() {

    private val rtdb = FirebaseDatabase.getInstance()
    private val connectedRef = rtdb.getReference(".info/connected")
    private var connectedListener: ValueEventListener? = null
    private var ownUserId: String? = null

    fun startTracking(userId: String) {
        if (ownUserId == userId) return
        stopTracking()
        ownUserId = userId
        val statusRef = rtdb.getReference("status/$userId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.getValue(Boolean::class.java) == true) {
                    // Queue disconnect handler first, then go online
                    statusRef.onDisconnect().setValue("offline")
                        .addOnSuccessListener { statusRef.setValue("online") }
                        .addOnFailureListener { e ->
                            Log.e("PresenceManager", "onDisconnect setup failed: ${e.message}")
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PresenceManager", "Connection listener cancelled: ${error.message}")
            }
        }
        connectedListener = listener
        connectedRef.addValueEventListener(listener)
    }

    fun goOffline() {
        ownUserId?.let { rtdb.getReference("status/$it").setValue("offline") }
    }

    fun stopTracking() {
        connectedListener?.let { connectedRef.removeEventListener(it) }
        connectedListener = null
        goOffline()
        ownUserId = null
    }

    fun observeUserStatus(userId: String): Flow<Boolean> = callbackFlow {
        val ref = rtdb.getReference("status/$userId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(String::class.java) == "online")
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
