package com.christian.quickcart

/**
 * Represents one recipe suggestion returned by the HTTP recipe request.
 */
data class RecipeSuggestion(
    val name: String,
    val sourceText: String,
    val sourceUrl: String
)
