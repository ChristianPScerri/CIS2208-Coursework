package com.christian.quickcart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.christian.quickcart.databinding.FragmentRecipesBinding
import com.google.android.material.chip.Chip
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Recipes fragment that fetches suggestions from a web API.
 */
class RecipesFragment : Fragment() {

    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: ShoppingDatabaseHelper
    private lateinit var recipeListAdapter: RecipeListAdapter
    private var pantryIngredientNames = listOf<String>()
    private val selectedIngredientNames = mutableSetOf<String>()
    private var latestRecipeRequestId = 0

    /**
     * Creates the recipes screen view using generated view binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Prepares recipe loading controls.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = ShoppingDatabaseHelper(requireContext())
        databaseHelper.seedSamplePantryItemsIfEmpty()
        recipeListAdapter = RecipeListAdapter(emptyList())
        binding.recyclerviewRecipes.adapter = recipeListAdapter
        populateIngredientChips()

        binding.buttonFetchRecipes.setOnClickListener {
            fetchRecipesFromPantry()
        }
        binding.edittextSearchRecipeIngredients.addTextChangedListener { searchText ->
            showIngredientChips(searchText.toString())
        }
    }

    /**
     * Refreshes pantry choices when returning from the pantry form.
     */
    override fun onResume() {
        super.onResume()
        if (::databaseHelper.isInitialized) {
            populateIngredientChips()
        }
    }

    /**
     * Loads pantry item names into selectable chips.
     */
    private fun populateIngredientChips() {
        pantryIngredientNames = databaseHelper.getAllPantryItems()
            .map { it.name }
            .distinct()

        selectedIngredientNames.retainAll(pantryIngredientNames.toSet())
        showIngredientChips(binding.edittextSearchRecipeIngredients.text.toString())
        binding.buttonFetchRecipes.isEnabled = pantryIngredientNames.isNotEmpty()

        if (pantryIngredientNames.isEmpty()) {
            binding.textviewRecipeStatus.setText(R.string.recipes_no_pantry)
        }
    }

    /**
     * Displays pantry ingredient chips filtered by the ingredient search field.
     */
    private fun showIngredientChips(searchText: String) {
        val filteredIngredients = pantryIngredientNames.filter { ingredient ->
            ingredient.contains(searchText, ignoreCase = true)
        }

        binding.chipgroupPantryIngredients.removeAllViews()
        filteredIngredients.forEach { ingredient ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = ingredient
                isCheckable = true
                isChecked = selectedIngredientNames.contains(ingredient)
                tag = ingredient
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedIngredientNames.add(ingredient)
                    } else {
                        selectedIngredientNames.remove(ingredient)
                    }
                }
            }
            binding.chipgroupPantryIngredients.addView(chip)
        }
    }

    /**
     * Reads selected pantry ingredients and starts a more specific HTTP recipe search.
     */
    private fun fetchRecipesFromPantry() {
        val selectedIngredients = getSelectedIngredients()

        if (selectedIngredients.isEmpty()) {
            binding.textviewRecipeStatus.setText(R.string.recipes_select_ingredient)
            recipeListAdapter.submitRecipes(emptyList())
            return
        }

        binding.textviewRecipeStatus.setText(R.string.recipes_loading)
        binding.buttonFetchRecipes.isEnabled = false
        val requestId = ++latestRecipeRequestId

        Thread {
            val recipes = loadRecipesForIngredients(selectedIngredients)
            activity?.runOnUiThread {
                if (_binding == null || requestId != latestRecipeRequestId) {
                    return@runOnUiThread
                }

                binding.buttonFetchRecipes.isEnabled = pantryIngredientNames.isNotEmpty()
                if (recipes == null) {
                    binding.textviewRecipeStatus.setText(R.string.recipes_error)
                    recipeListAdapter.submitRecipes(emptyList())
                } else {
                    val ingredientText = selectedIngredients.joinToString(", ")
                    binding.textviewRecipeStatus.text = if (recipes.isEmpty()) {
                        getString(R.string.recipes_no_matches, ingredientText)
                    } else {
                        getString(R.string.recipes_showing, ingredientText)
                    }
                    recipeListAdapter.submitRecipes(recipes)
                }
            }
        }.start()
    }

    /**
     * Returns all currently selected pantry ingredient chips.
     */
    private fun getSelectedIngredients(): List<String> {
        return selectedIngredientNames.toList()
    }

    /**
     * Finds candidate meals, loads full meal details, and ranks by actual ingredient matches.
     */
    private fun loadRecipesForIngredients(ingredients: List<String>): List<RecipeSuggestion>? {
        return try {
            val candidates = linkedMapOf<String, RecipeCandidate>()

            ingredients.forEach { ingredient ->
                loadMealsForIngredient(ingredient).forEach { meal ->
                    val candidate = candidates.getOrPut(meal.id) {
                        RecipeCandidate(
                            id = meal.id,
                            name = meal.name,
                            quickMatchedIngredients = mutableSetOf()
                        )
                    }
                    candidate.quickMatchedIngredients.add(ingredient)
                }
            }

            candidates.values
                .sortedByDescending { it.quickMatchedIngredients.size }
                .take(MAX_DETAIL_LOOKUPS)
                .mapNotNull { candidate ->
                    val details = loadMealDetails(candidate.id) ?: return@mapNotNull null
                    val matchedIngredients = findMatchingIngredients(ingredients, details.ingredients)

                    RecipeSuggestion(
                        name = details.name,
                        sourceText = buildMatchText(matchedIngredients, ingredients.size),
                        sourceUrl = "https://www.themealdb.com/meal/${candidate.id}"
                    )
                }
                .sortedWith(
                    compareByDescending<RecipeSuggestion> { extractMatchCount(it.sourceText) }
                        .thenBy { it.name }
                )
                .take(MAX_RECIPE_RESULTS)
        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Calls TheMealDB filter endpoint for one ingredient.
     */
    private fun loadMealsForIngredient(ingredient: String): List<MealResult> {
        val encodedIngredient = URLEncoder.encode(ingredient, "UTF-8")
        val url = URL("https://www.themealdb.com/api/json/v1/1/filter.php?i=$encodedIngredient")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val meals = JSONObject(response).optJSONArray("meals") ?: return emptyList()
        val results = mutableListOf<MealResult>()

        for (index in 0 until meals.length()) {
            val meal = meals.getJSONObject(index)
            results.add(
                MealResult(
                    id = meal.getString("idMeal"),
                    name = meal.getString("strMeal")
                )
            )
        }

        return results
    }

    /**
     * Loads full recipe details so matching can use the actual ingredient list.
     */
    private fun loadMealDetails(mealId: String): MealDetails? {
        val url = URL("https://www.themealdb.com/api/json/v1/1/lookup.php?i=$mealId")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val meal = JSONObject(response).optJSONArray("meals")?.optJSONObject(0) ?: return null
        val ingredients = mutableListOf<String>()

        for (index in 1..20) {
            val ingredient = meal.optString("strIngredient$index").trim()
            if (ingredient.isNotBlank()) {
                ingredients.add(ingredient)
            }
        }

        return MealDetails(
            name = meal.getString("strMeal"),
            ingredients = ingredients
        )
    }

    /**
     * Matches selected pantry ingredients against the full recipe ingredient list.
     */
    private fun findMatchingIngredients(
        selectedIngredients: List<String>,
        recipeIngredients: List<String>
    ): List<String> {
        return selectedIngredients.filter { selected ->
            recipeIngredients.any { recipeIngredient ->
                recipeIngredient.contains(selected, ignoreCase = true) ||
                    selected.contains(recipeIngredient, ignoreCase = true)
            }
        }
    }

    /**
     * Builds a concise explanation of why a recipe is suggested.
     */
    private fun buildMatchText(matchedIngredients: List<String>, selectedCount: Int): String {
        val matchedText = matchedIngredients.sorted().joinToString(", ")
        return if (matchedIngredients.isEmpty()) {
            "Related recipe"
        } else {
            "Matches ${matchedIngredients.size}/$selectedCount: $matchedText"
        }
    }

    /**
     * Extracts match count from the display text for final sorting.
     */
    private fun extractMatchCount(sourceText: String): Int {
        return Regex("""Matches (\d+)/""")
            .find(sourceText)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: 0
    }

    /**
     * Lightweight meal result returned from the ingredient filter endpoint.
     */
    private data class MealResult(
        val id: String,
        val name: String
    )

    /**
     * Full meal details returned by TheMealDB lookup endpoint.
     */
    private data class MealDetails(
        val name: String,
        val ingredients: List<String>
    )

    /**
     * Combined recipe candidate built from one or more ingredient searches.
     */
    private data class RecipeCandidate(
        val id: String,
        val name: String,
        val quickMatchedIngredients: MutableSet<String>
    )

    /**
     * Clears the view binding reference when the fragment view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        latestRecipeRequestId++
        _binding = null
    }

    companion object {
        private const val MAX_RECIPE_RESULTS = 30
        private const val MAX_DETAIL_LOOKUPS = 40
    }
}
