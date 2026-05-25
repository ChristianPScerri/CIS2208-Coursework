package com.christian.quickcart

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.christian.quickcart.databinding.FragmentRecipesBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Recipes fragment that fetches suggestions from a web API and handles image intents.
 */
class RecipesFragment : Fragment() {

    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: ShoppingDatabaseHelper
    private lateinit var recipeListAdapter: RecipeListAdapter

    private val galleryImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { imageUri ->
        imageUri?.let {
            binding.imageviewReceiptPreview.setImageURI(it)
            binding.textviewImageStatus.setText(R.string.image_selected)
        }
    }

    private val cameraImageLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            binding.imageviewReceiptPreview.setImageBitmap(it)
            binding.textviewImageStatus.setText(R.string.image_selected)
        }
    }

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
     * Prepares recipe loading and image upload controls.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = ShoppingDatabaseHelper(requireContext())
        recipeListAdapter = RecipeListAdapter(emptyList())
        binding.recyclerviewRecipes.adapter = recipeListAdapter

        binding.buttonFetchRecipes.setOnClickListener {
            fetchRecipesFromPantry()
        }
        binding.buttonChooseGalleryImage.setOnClickListener {
            galleryImageLauncher.launch("image/*")
        }
        binding.buttonTakeCameraPhoto.setOnClickListener {
            cameraImageLauncher.launch(null)
        }
    }

    /**
     * Finds a pantry ingredient and starts an HTTP recipe search.
     */
    private fun fetchRecipesFromPantry() {
        val ingredient = databaseHelper.getAllPantryItems().firstOrNull()?.name

        if (ingredient.isNullOrBlank()) {
            binding.textviewRecipeStatus.setText(R.string.recipes_no_pantry)
            recipeListAdapter.submitRecipes(emptyList())
            return
        }

        binding.textviewRecipeStatus.setText(R.string.recipes_loading)

        Thread {
            val recipes = loadRecipesForIngredient(ingredient)
            activity?.runOnUiThread {
                if (recipes == null) {
                    binding.textviewRecipeStatus.setText(R.string.recipes_error)
                    recipeListAdapter.submitRecipes(emptyList())
                } else {
                    binding.textviewRecipeStatus.text =
                        "Showing recipes using $ingredient"
                    recipeListAdapter.submitRecipes(recipes)
                }
            }
        }.start()
    }

    /**
     * Calls TheMealDB ingredient endpoint and converts the JSON response into recipe rows.
     */
    private fun loadRecipesForIngredient(ingredient: String): List<RecipeSuggestion>? {
        return try {
            val encodedIngredient = URLEncoder.encode(ingredient, "UTF-8")
            val url = URL("https://www.themealdb.com/api/json/v1/1/filter.php?i=$encodedIngredient")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val meals = JSONObject(response).optJSONArray("meals") ?: return emptyList()
            val suggestions = mutableListOf<RecipeSuggestion>()

            for (index in 0 until meals.length()) {
                val meal = meals.getJSONObject(index)
                suggestions.add(
                    RecipeSuggestion(
                        name = meal.getString("strMeal"),
                        sourceText = "Suggested from pantry ingredient"
                    )
                )
            }

            suggestions
        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Clears the view binding reference when the fragment view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
