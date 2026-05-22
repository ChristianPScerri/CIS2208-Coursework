package com.christian.quickcart

/**
 * Represents one shopping list item shown in the RecyclerView.
 */
data class ShoppingItem(
    val name: String,
    val quantity: String,
    val category: String,
    val priority: String,
    var isBought: Boolean = false
)
