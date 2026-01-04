package com.rajatt7z.retailx.models

data class LoginLog(
    val logId: String = "",
    val userId: String = "",
    val userType: String = "",
    val email: String = "",
    val timestamp: Long = 0L,
    val deviceName: String = ""
)
