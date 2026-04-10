package com.rajatt7z.retailx.models

data class ReturnItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val refundAmount: Double = 0.0
)
