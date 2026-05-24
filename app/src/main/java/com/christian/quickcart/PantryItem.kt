package com.christian.quickcart

/**
 * Represents one pantry item saved locally on the device.
 */
data class PantryItem(
    val id: Long,
    val name: String,
    val quantity: String,
    val category: String,
    val expiryDate: String
)
