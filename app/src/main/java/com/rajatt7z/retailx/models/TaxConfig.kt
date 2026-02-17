package com.rajatt7z.retailx.models

data class TaxConfig(
    val name: String = "Tax",
    val rate: Double = 0.0,
    val isEnabled: Boolean = false,
    val currency: String = "$"
)
