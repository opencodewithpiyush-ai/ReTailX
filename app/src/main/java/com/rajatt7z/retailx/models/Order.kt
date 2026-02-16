package com.rajatt7z.retailx.models

data class Order(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val totalPrice: Double = 0.0,
    val timestamp: Long = 0L,
    val status: String = "Pending", // Pending, Completed, Cancelled
    val soldBy: String = "", // Employee ID
    val customerName: String = "",
    val customerPhone: String = ""
)
