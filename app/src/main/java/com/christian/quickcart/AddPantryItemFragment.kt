package com.christian.quickcart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.christian.quickcart.databinding.FragmentAddPantryItemBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment that lets the user add or edit a pantry item in SQLite.
 */
class AddPantryItemFragment : Fragment() {

    private var _binding: FragmentAddPantryItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: ShoppingDatabaseHelper
    private var editingPantryItemId: Long = NO_ITEM_ID

    /**
     * Creates the pantry item form view using generated view binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPantryItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Prepares the database helper and connects the save button.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = ShoppingDatabaseHelper(requireContext())
        editingPantryItemId =
            arguments?.getLong(ARG_PANTRY_ITEM_ID, NO_ITEM_ID) ?: NO_ITEM_ID

        if (editingPantryItemId != NO_ITEM_ID) {
            loadPantryItemForEditing(editingPantryItemId)
        }

        binding.buttonSavePantryItem.setOnClickListener {
            savePantryItem()
        }
    }

    /**
     * Loads an existing pantry item into the form for editing.
     */
    private fun loadPantryItemForEditing(itemId: Long) {
        val item = databaseHelper.getPantryItemById(itemId) ?: return

        binding.textviewAddPantryTitle.setText(R.string.edit_pantry_item_title)
        binding.buttonSavePantryItem.setText(R.string.update_pantry_item)
        binding.edittextPantryName.setText(item.name)
        binding.edittextPantryQuantity.setText(item.quantity)
        binding.edittextPantryCategory.setText(item.category)
        binding.edittextPantryExpiry.setText(item.expiryDate)
    }

    /**
     * Validates the form and saves the pantry item to SQLite.
     */
    private fun savePantryItem() {
        val name = binding.edittextPantryName.text.toString().trim()

        if (name.isEmpty()) {
            Snackbar.make(binding.root, R.string.pantry_item_name_required, Snackbar.LENGTH_LONG)
                .show()
            return
        }

        val quantity = binding.edittextPantryQuantity.text.toString().trim()
            .ifEmpty { getString(R.string.quantity_default) }
        val category = binding.edittextPantryCategory.text.toString().trim()
            .ifEmpty { getString(R.string.category_default) }
        val expiryDate = binding.edittextPantryExpiry.text.toString().trim()
            .ifEmpty { getString(R.string.expiry_default) }

        if (editingPantryItemId == NO_ITEM_ID) {
            databaseHelper.insertPantryItem(name, quantity, category, expiryDate)
            Snackbar.make(binding.root, R.string.pantry_item_saved, Snackbar.LENGTH_SHORT).show()
        } else {
            databaseHelper.updatePantryItem(
                editingPantryItemId,
                name,
                quantity,
                category,
                expiryDate
            )
            Snackbar.make(binding.root, R.string.pantry_item_updated, Snackbar.LENGTH_SHORT).show()
        }

        findNavController().popBackStack()
    }

    /**
     * Clears the view binding reference when the fragment view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_PANTRY_ITEM_ID = "pantry_item_id"
        private const val NO_ITEM_ID = -1L
    }
}
