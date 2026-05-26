package com.christian.quickcart

/**
 * Represents one shopping list item shown in the RecyclerView.
 */
data class ShoppingItem(
    val id: Long,
    val name: String,
    val quantity: String,
    val category: String,
    val priority: String,
    val notes: String,
    val amount: Int = 1,
    val imagePath: String = "",
    var isBought: Boolean = false
)
