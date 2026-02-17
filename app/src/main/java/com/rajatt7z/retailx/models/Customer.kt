package com.rajatt7z.retailx.models

data class Customer(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val createdAt: Long = 0L,
    val totalOrders: Int = 0,
    val totalSpent: Double = 0.0
)
