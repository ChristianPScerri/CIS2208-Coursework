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
        binding.buttonSaveItem.setOnClickListener {
            saveShoppingItem()
        }
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

        databaseHelper.insertShoppingItem(name, quantity, category, priority, notes)
        Snackbar.make(binding.root, R.string.item_saved, Snackbar.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    /**
     * Clears the view binding reference when the fragment view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
