package com.christian.quickcart

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.christian.quickcart.databinding.FragmentAddItemBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File

/**
 * Fragment that lets the user create a new shopping item and save it locally.
 */
class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: ShoppingDatabaseHelper
    private var editingItemId: Long = NO_ITEM_ID
    private var selectedImagePath: String = ""

    private val productImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { imageUri ->
        imageUri?.let {
            selectedImagePath = saveProductImage(it)
            showProductImage(selectedImagePath)
            Snackbar.make(binding.root, R.string.product_image_selected, Snackbar.LENGTH_SHORT).show()
        }
    }

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
        setupCategorySpinner()
        setupPrioritySpinner()

        if (editingItemId != NO_ITEM_ID) {
            loadShoppingItemForEditing(editingItemId)
        }

        binding.buttonChooseItemImage.setOnClickListener {
            productImageLauncher.launch("image/*")
        }
        binding.containerItemImage.setOnClickListener {
            productImageLauncher.launch("image/*")
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

        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.edit_item_title)
        binding.buttonSaveItem.setText(R.string.update_item)
        binding.edittextItemName.setText(item.name)
        binding.edittextItemQuantity.setText(item.quantity)
        binding.edittextItemAmount.setText(item.amount.toString())
        selectSpinnerValue(binding.spinnerItemCategory, item.category)
        selectSpinnerValue(binding.spinnerItemPriority, item.priority)
        binding.edittextItemNotes.setText(item.notes)
        selectedImagePath = item.imagePath
        showProductImage(selectedImagePath)
    }

    /**
     * Copies the chosen product image into private storage and returns its file path.
     */
    private fun saveProductImage(imageUri: android.net.Uri): String {
        val imageFile = File(requireContext().filesDir, "shopping_item_${System.currentTimeMillis()}.jpg")
        requireContext().contentResolver.openInputStream(imageUri)?.use { inputStream ->
            imageFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return imageFile.absolutePath
    }

    /**
     * Displays the chosen product image if one has been saved for this item.
     */
    private fun showProductImage(imagePath: String) {
        if (imagePath.isBlank()) {
            binding.imageviewItemPreview.setImageDrawable(null)
            binding.layoutItemImageEmpty.visibility = View.VISIBLE
            return
        }

        BitmapFactory.decodeFile(imagePath)?.let {
            binding.imageviewItemPreview.setImageBitmap(it)
            binding.layoutItemImageEmpty.visibility = View.GONE
        } ?: run {
            binding.imageviewItemPreview.setImageDrawable(null)
            binding.layoutItemImageEmpty.visibility = View.VISIBLE
        }
    }

    /**
     * Provides common shopping categories so users can add items faster.
     */
    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.item_categories,
            R.layout.item_spinner_selected
        )
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerItemCategory.adapter = adapter
    }

    /**
     * Provides a fixed set of priorities for consistent shopping list rows.
     */
    private fun setupPrioritySpinner() {
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.item_priorities,
            R.layout.item_spinner_selected
        )
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerItemPriority.adapter = adapter
        selectSpinnerValue(binding.spinnerItemPriority, getString(R.string.priority_default))
    }

    /**
     * Selects an existing spinner value when an item is opened for editing.
     */
    private fun selectSpinnerValue(spinner: android.widget.Spinner, value: String) {
        val adapter = spinner.adapter ?: return

        for (index in 0 until adapter.count) {
            if (adapter.getItem(index).toString() == value) {
                spinner.setSelection(index)
                return
            }
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
        val amount = binding.edittextItemAmount.text.toString().toIntOrNull()
            ?.coerceAtLeast(1)
            ?: DEFAULT_AMOUNT
        val category = binding.spinnerItemCategory.selectedItem as? String
            ?: getString(R.string.category_default)
        val priority = binding.spinnerItemPriority.selectedItem as? String
            ?: getString(R.string.priority_default)
        val notes = binding.edittextItemNotes.text.toString().trim()

        if (editingItemId == NO_ITEM_ID) {
            databaseHelper.insertShoppingItem(name, quantity, category, priority, notes, amount, selectedImagePath)
            Snackbar.make(binding.root, R.string.item_saved, Snackbar.LENGTH_SHORT).show()
        } else {
            databaseHelper.updateShoppingItem(
                editingItemId,
                name,
                quantity,
                category,
                priority,
                notes,
                amount,
                selectedImagePath
            )
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
        private const val DEFAULT_AMOUNT = 1
    }
}
