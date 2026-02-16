package com.rajatt7z.retailx.models

import com.google.firebase.Timestamp

data class Bill(
    val id: String = "",
    val products: List<CartItem> = emptyList(),
    val customerName: String = "",
    val customerPhone: String = "",
    val totalAmount: Double = 0.0,
    val timestamp: Long = 0L,
    val generatedBy: String = "", // Employee ID
    val generatedByName: String = "" // Employee Name for display on Bill
)
