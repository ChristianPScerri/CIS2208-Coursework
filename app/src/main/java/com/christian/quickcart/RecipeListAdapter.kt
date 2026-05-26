package com.christian.quickcart

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.christian.quickcart.databinding.ItemRecipeBinding

/**
 * RecyclerView adapter that displays recipe suggestions.
 */
class RecipeListAdapter(
    private var recipes: List<RecipeSuggestion>
) : RecyclerView.Adapter<RecipeListAdapter.RecipeViewHolder>() {

    /**
     * Creates a view holder for one recipe row.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecipeViewHolder(binding)
    }

    /**
     * Binds the recipe at the requested position.
     */
    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(recipes[position])
    }

    /**
     * Returns the number of recipes currently displayed.
     */
    override fun getItemCount(): Int = recipes.size

    /**
     * Replaces the displayed recipe suggestions.
     */
    fun submitRecipes(updatedRecipes: List<RecipeSuggestion>) {
        recipes = updatedRecipes
        notifyDataSetChanged()
    }

    /**
     * View holder that owns one recipe row.
     */
    class RecipeViewHolder(
        private val binding: ItemRecipeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Shows the recipe title and source text.
         */
        fun bind(recipe: RecipeSuggestion) {
            binding.textviewRecipeName.text = recipe.name
            binding.textviewRecipeSource.text = recipe.sourceText
            binding.root.setOnClickListener {
                openRecipe(recipe)
            }
            binding.textviewOpenRecipe.setOnClickListener {
                openRecipe(recipe)
            }
        }

        /**
         * Opens the recipe web page in the user's browser.
         */
        private fun openRecipe(recipe: RecipeSuggestion) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(recipe.sourceUrl))
                binding.root.context.startActivity(intent)
            } catch (exception: ActivityNotFoundException) {
                // If no browser is available, the row simply remains on screen.
            }
        }
    }
}
