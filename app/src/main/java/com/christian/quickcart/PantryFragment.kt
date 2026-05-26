package com.christian.quickcart

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.christian.quickcart.databinding.FragmentPantryBinding
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDate
import java.time.format.DateTimeParseException

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
        databaseHelper.seedSamplePantryItemsIfEmpty()
        pantryItems = sortPantryItems(databaseHelper.getAllPantryItems())

        pantryListAdapter = PantryListAdapter(
            pantryItems,
            onEditClicked = { item ->
                openEditPantryItemScreen(item)
            },
            onAddExpiryClicked = { item ->
                showAddExpiryPicker(item)
            },
            onDeleteClicked = { item ->
                deletePantryItem(item)
            }
        )
        binding.recyclerviewPantryItems.adapter = pantryListAdapter
        updateExpiredSummary()
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
            pantryItems = sortPantryItems(pantryItems)
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

        val sortedItems = sortPantryItems(filteredItems)
        pantryListAdapter.submitItems(sortedItems)
        updateExpiredSummary()
        updateEmptyState(sortedItems)
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
        pantryItems = sortPantryItems(databaseHelper.getAllPantryItems())
        filterPantryItems(binding.edittextSearchPantry.text.toString())
        Snackbar.make(binding.root, R.string.pantry_item_deleted, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * Lets the user add an expiry date directly from a pantry row that has no date.
     */
    private fun showAddExpiryPicker(item: PantryItem) {
        val today = LocalDate.now()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth).toString()
                databaseHelper.updatePantryItem(
                    itemId = item.id,
                    name = item.name,
                    quantity = item.quantity,
                    category = item.category,
                    expiryDate = selectedDate,
                    amount = item.amount,
                    imagePath = item.imagePath
                )
                pantryItems = sortPantryItems(databaseHelper.getAllPantryItems())
                filterPantryItems(binding.edittextSearchPantry.text.toString())
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth
        ).show()
    }

    /**
     * Sorts pantry rows so expired and soonest-expiring items are easiest to notice.
     */
    private fun sortPantryItems(items: List<PantryItem>): List<PantryItem> {
        return items.sortedWith(
            compareBy<PantryItem> { expirySortGroup(it) }
                .thenBy { parseExpiryDate(it.expiryDate) ?: LocalDate.MAX }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Places expired rows first, dated rows next, and unknown dates last.
     */
    private fun expirySortGroup(item: PantryItem): Int {
        val expiryDate = parseExpiryDate(item.expiryDate) ?: return 2
        return if (expiryDate.isBefore(LocalDate.now())) 0 else 1
    }

    /**
     * Reads dates stored by the pantry date picker.
     */
    private fun parseExpiryDate(expiryDate: String): LocalDate? {
        return try {
            LocalDate.parse(expiryDate)
        } catch (exception: DateTimeParseException) {
            null
        }
    }

    /**
     * Updates the expiry summary badge above the pantry search box.
     */
    private fun updateExpiredSummary() {
        val expiredCount = pantryItems.count { item ->
            parseExpiryDate(item.expiryDate)?.isBefore(LocalDate.now()) == true
        }
        binding.textviewPantryExpiredSummary.text =
            getString(R.string.pantry_expired_summary, expiredCount)
        binding.textviewPantryExpiredSummary.setTextColor(
            if (expiredCount > 0) android.graphics.Color.WHITE
            else android.graphics.Color.parseColor("#1B5E20")
        )
        binding.textviewPantryExpiredSummary.setBackgroundColor(
            if (expiredCount > 0) android.graphics.Color.parseColor("#C62828")
            else android.graphics.Color.parseColor("#E8F5E9")
        )
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
