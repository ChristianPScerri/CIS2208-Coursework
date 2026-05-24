package com.christian.quickcart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.christian.quickcart.databinding.FragmentPantryBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Pantry fragment that displays saved pantry items in a RecyclerView.
 */
class PantryFragment : Fragment() {

    private var _binding: FragmentPantryBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: ShoppingDatabaseHelper
    private lateinit var pantryListAdapter: PantryListAdapter
    private var pantryItems = listOf<PantryItem>()

    /**
     * Creates the pantry screen view using generated view binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPantryBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Loads pantry items and connects search, edit, and delete actions.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = ShoppingDatabaseHelper(requireContext())
        pantryItems = databaseHelper.getAllPantryItems()

        pantryListAdapter = PantryListAdapter(
            pantryItems,
            onEditClicked = { item ->
                openEditPantryItemScreen(item)
            },
            onDeleteClicked = { item ->
                deletePantryItem(item)
            }
        )
        binding.recyclerviewPantryItems.adapter = pantryListAdapter
        updateEmptyState(pantryItems)

        binding.edittextSearchPantry.addTextChangedListener { searchText ->
            filterPantryItems(searchText.toString())
        }
    }

    /**
     * Reloads saved pantry items when returning from the Add Pantry Item screen.
     */
    override fun onResume() {
        super.onResume()
        if (::databaseHelper.isInitialized && ::pantryListAdapter.isInitialized) {
            pantryItems = databaseHelper.getAllPantryItems()
            filterPantryItems(binding.edittextSearchPantry.text.toString())
        }
    }

    /**
     * Filters pantry items by name or category and updates the empty state.
     */
    private fun filterPantryItems(searchText: String) {
        val filteredItems = pantryItems.filter { item ->
            item.name.contains(searchText, ignoreCase = true) ||
                item.category.contains(searchText, ignoreCase = true)
        }

        pantryListAdapter.submitItems(filteredItems)
        updateEmptyState(filteredItems)
    }

    /**
     * Opens the pantry item form in edit mode.
     */
    private fun openEditPantryItemScreen(item: PantryItem) {
        val arguments = Bundle().apply {
            putLong(AddPantryItemFragment.ARG_PANTRY_ITEM_ID, item.id)
        }
        findNavController().navigate(R.id.AddPantryItemFragment, arguments)
    }

    /**
     * Deletes a pantry item from SQLite and refreshes the visible list.
     */
    private fun deletePantryItem(item: PantryItem) {
        databaseHelper.deletePantryItem(item.id)
        pantryItems = databaseHelper.getAllPantryItems()
        filterPantryItems(binding.edittextSearchPantry.text.toString())
        Snackbar.make(binding.root, R.string.pantry_item_deleted, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Shows an empty state when there are no pantry rows to display.
     */
    private fun updateEmptyState(items: List<PantryItem>) {
        binding.textviewEmptyPantry.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * Clears the view binding reference when the fragment view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
