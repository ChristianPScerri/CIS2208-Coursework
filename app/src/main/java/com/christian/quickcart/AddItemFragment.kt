package com.christian.quickcart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.christian.quickcart.databinding.FragmentAddItemBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment that lets the user create a new shopping item and save it locally.
 */
class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: ShoppingDatabaseHelper
    private var editingItemId: Long = NO_ITEM_ID

    /**
     * Creates the add item form view using generated view binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Prepares the database helper and connects the save button to validation.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = ShoppingDatabaseHelper(requireContext())
        editingItemId = arguments?.getLong(ARG_ITEM_ID, NO_ITEM_ID) ?: NO_ITEM_ID

        if (editingItemId != NO_ITEM_ID) {
            loadShoppingItemForEditing(editingItemId)
        }

        binding.buttonSaveItem.setOnClickListener {
            saveShoppingItem()
        }
    }

    /**
     * Loads an existing shopping item into the form for editing.
     */
    private fun loadShoppingItemForEditing(itemId: Long) {
        val item = databaseHelper.getShoppingItemById(itemId) ?: return

        binding.textviewAddItemTitle.setText(R.string.edit_item_title)
        binding.buttonSaveItem.setText(R.string.update_item)
        binding.edittextItemName.setText(item.name)
        binding.edittextItemQuantity.setText(item.quantity)
        binding.edittextItemCategory.setText(item.category)
        binding.edittextItemPriority.setText(item.priority)
        binding.edittextItemNotes.setText(item.notes)
    }

    /**
     * Validates the form, saves the new item to SQLite, and returns to the previous screen.
     */
    private fun saveShoppingItem() {
        val name = binding.edittextItemName.text.toString().trim()

        if (name.isEmpty()) {
            Snackbar.make(binding.root, R.string.item_name_required, Snackbar.LENGTH_LONG).show()
            return
        }

        val quantity = binding.edittextItemQuantity.text.toString().trim()
            .ifEmpty { getString(R.string.quantity_default) }
        val category = binding.edittextItemCategory.text.toString().trim()
            .ifEmpty { getString(R.string.category_default) }
        val priority = binding.edittextItemPriority.text.toString().trim()
            .ifEmpty { getString(R.string.priority_default) }
        val notes = binding.edittextItemNotes.text.toString().trim()

        if (editingItemId == NO_ITEM_ID) {
            databaseHelper.insertShoppingItem(name, quantity, category, priority, notes)
            Snackbar.make(binding.root, R.string.item_saved, Snackbar.LENGTH_SHORT).show()
        } else {
            databaseHelper.updateShoppingItem(editingItemId, name, quantity, category, priority, notes)
            Snackbar.make(binding.root, R.string.item_updated, Snackbar.LENGTH_SHORT).show()
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
        const val ARG_ITEM_ID = "item_id"
        private const val NO_ITEM_ID = -1L
    }
}
