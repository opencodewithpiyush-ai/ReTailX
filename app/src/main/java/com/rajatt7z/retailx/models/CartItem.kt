package com.rajatt7z.retailx.models

data class CartItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val discount: Double = 0.0,         // Amount discounted
    val discountType: String = "none",  // "none", "percentage", "fixed"
    val discountValue: Double = 0.0,    // Raw input value (e.g. 10 for 10% or $10)
    val totalPrice: Double = 0.0
)
