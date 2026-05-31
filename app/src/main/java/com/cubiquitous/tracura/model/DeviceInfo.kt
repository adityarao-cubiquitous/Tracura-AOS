package com.cubiquitous.tracura.model

data class DeviceInfo(
    val fcmToken: String = "",
    val deviceId: String = "",
    val deviceModel: String = "",
    val osVersion: String = "",
    val appVersion: String = "",
    val lastLoginAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true
) 