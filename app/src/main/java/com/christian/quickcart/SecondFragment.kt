package com.christian.quickcart

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import com.christian.quickcart.databinding.FragmentSecondBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Shopping list fragment that displays sample items in a RecyclerView.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private lateinit var shoppingListAdapter: ShoppingListAdapter
    private lateinit var databaseHelper: ShoppingDatabaseHelper
    private var shoppingItems = listOf<ShoppingItem>()

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    /**
     * Creates the shopping list placeholder view using generated view binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    /**
     * Connects the placeholder back button to the home dashboard.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = ShoppingDatabaseHelper(requireContext())
        databaseHelper.seedSampleItemsIfEmpty()
        shoppingItems = databaseHelper.getAllShoppingItems()

        shoppingListAdapter = ShoppingListAdapter(
            shoppingItems,
            onBoughtChanged = { item, isBought ->
                databaseHelper.updateBoughtStatus(item.id, isBought)
            },
            onEditClicked = { item ->
                openEditItemScreen(item)
            },
            onDeleteClicked = { item ->
                deleteShoppingItem(item)
            }
        )
        binding.recyclerviewShoppingItems.adapter = shoppingListAdapter

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        binding.edittextSearchItems.addTextChangedListener { searchText ->
            filterShoppingItems(searchText.toString())
        }
    }

    /**
     * Reloads saved items when the user returns from the Add Item screen.
     */
    override fun onResume() {
        super.onResume()
        if (::databaseHelper.isInitialized && ::shoppingListAdapter.isInitialized) {
            shoppingItems = databaseHelper.getAllShoppingItems()
            filterShoppingItems(binding.edittextSearchItems.text.toString())
        }
    }

    /**
     * Filters the sample shopping items by name or category and updates the empty state.
     */
    private fun filterShoppingItems(searchText: String) {
        val filteredItems = shoppingItems.filter { item ->
            item.name.contains(searchText, ignoreCase = true) ||
                item.category.contains(searchText, ignoreCase = true)
        }

        shoppingListAdapter.submitItems(filteredItems)
        binding.textviewEmptyList.visibility =
            if (filteredItems.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * Opens the add item form in edit mode for the selected shopping item.
     */
    private fun openEditItemScreen(item: ShoppingItem) {
        val arguments = Bundle().apply {
            putLong(AddItemFragment.ARG_ITEM_ID, item.id)
        }
        findNavController().navigate(R.id.AddItemFragment, arguments)
    }

    /**
     * Deletes the selected shopping item and refreshes the visible list.
     */
    private fun deleteShoppingItem(item: ShoppingItem) {
        databaseHelper.deleteShoppingItem(item.id)
        shoppingItems = databaseHelper.getAllShoppingItems()
        filterShoppingItems(binding.edittextSearchItems.text.toString())
        Snackbar.make(binding.root, R.string.item_deleted, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Clears the view binding reference when the fragment view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
