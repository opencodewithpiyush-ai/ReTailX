package com.rajatt7z.retailx.models

data class Refund(
    val id: String = "",
    val originalBillId: String = "",
    val returnedItems: List<ReturnItem> = emptyList(),
    val refundAmount: Double = 0.0,
    val reason: String = "",
    val processedBy: String = "",       // Employee UID
    val processedByName: String = "",   // Employee Name for display
    val timestamp: Long = 0L,
    val status: String = "Processed"    // Processed, Pending, Rejected
)
