package com.rajatt7z.retailx.models

data class CartItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)
