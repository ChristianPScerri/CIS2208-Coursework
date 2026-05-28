package com.christian.quickcart

import android.content.Intent
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
     * Loads shopping items and connects search, share, edit, and delete actions.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = ShoppingDatabaseHelper(requireContext())
        databaseHelper.seedSampleItemsIfEmpty()
        shoppingItems = databaseHelper.getAllShoppingItems()

        shoppingListAdapter = ShoppingListAdapter(
            shoppingItems,
            onBoughtChanged = { item, isBought ->
                if (isBought) {
                    moveBoughtItemToPantry(item)
                } else {
                    databaseHelper.updateBoughtStatus(item.id, false)
                }
            },
            onEditClicked = { item ->
                openEditItemScreen(item)
            },
            onDeleteClicked = { item ->
                deleteShoppingItem(item)
            }
        )
        binding.recyclerviewShoppingItems.adapter = shoppingListAdapter

        binding.edittextSearchItems.addTextChangedListener { searchText ->
            filterShoppingItems(searchText.toString())
        }

        binding.buttonShareShoppingList.setOnClickListener {
            shareShoppingList()
        }

        binding.buttonEmptyAddShoppingItem.setOnClickListener {
            findNavController().navigate(R.id.AddItemFragment)
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
        val emptyVisibility = if (filteredItems.isEmpty()) View.VISIBLE else View.GONE
        binding.textviewEmptyList.visibility = emptyVisibility
        binding.buttonEmptyAddShoppingItem.visibility = emptyVisibility
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
     * Copies a bought shopping item into the pantry so purchased products become stock at home.
     */
    private fun moveBoughtItemToPantry(item: ShoppingItem) {
        databaseHelper.moveShoppingItemToPantry(
            item = item,
            expiryDate = getString(R.string.expiry_default)
        )
        shoppingItems = databaseHelper.getAllShoppingItems()
        filterShoppingItems(binding.edittextSearchItems.text.toString())
        Snackbar.make(binding.root, R.string.pantry_item_added_from_shopping, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Shares the saved shopping list through Android's share sheet.
     */
    private fun shareShoppingList() {
        val items = databaseHelper.getAllShoppingItems()

        if (items.isEmpty()) {
            Snackbar.make(binding.root, R.string.share_empty_list, Snackbar.LENGTH_SHORT).show()
            return
        }

        val listText = items.joinToString(separator = "\n") { item ->
            val boughtText = if (item.isBought) "Bought" else "Needed"
            "- ${item.name} (${item.amount} x ${item.quantity}, ${item.category}, $boughtText)"
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shopping_list_title))
            putExtra(Intent.EXTRA_TEXT, listText)
        }
        startActivity(
            Intent.createChooser(shareIntent, getString(R.string.share_chooser_title))
        )
    }

    /**
     * Clears the view binding reference when the fragment view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
