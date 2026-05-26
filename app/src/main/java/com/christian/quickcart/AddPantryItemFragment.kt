package com.christian.quickcart

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.christian.quickcart.databinding.FragmentAddPantryItemBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Fragment that lets the user add or edit a pantry item in SQLite.
 */
class AddPantryItemFragment : Fragment() {

    private var _binding: FragmentAddPantryItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: ShoppingDatabaseHelper
    private var editingPantryItemId: Long = NO_ITEM_ID
    private var selectedImagePath: String = ""

    private val productImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { imageUri ->
        imageUri?.let {
            if (_binding == null) return@registerForActivityResult
            selectedImagePath = saveProductImage(it)
            showProductImage(selectedImagePath)
            Snackbar.make(binding.root, R.string.product_image_selected, Snackbar.LENGTH_SHORT).show()
        }
    }

    private val cameraImageLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            if (_binding == null) return@registerForActivityResult
            selectedImagePath = saveCameraImage(it)
            showProductImage(selectedImagePath)
            Snackbar.make(binding.root, R.string.product_image_selected, Snackbar.LENGTH_SHORT).show()
        }
    }

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

        setupCategorySpinner()
        binding.edittextPantryExpiry.setOnClickListener {
            showExpiryDatePicker()
        }
        binding.buttonChoosePantryImage.setOnClickListener {
            showImageSourceDialog()
        }
        binding.containerPantryImage.setOnClickListener {
            showImageSourceDialog()
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

        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.edit_pantry_item_title)
        binding.buttonSavePantryItem.setText(R.string.update_pantry_item)
        binding.edittextPantryName.setText(item.name)
        binding.edittextPantryQuantity.setText(item.quantity)
        binding.edittextPantryAmount.setText(item.amount.toString())
        binding.edittextPantryExpiry.setText(item.expiryDate)
        selectedImagePath = item.imagePath
        showProductImage(selectedImagePath)
    }

    /**
     * Lets the user choose between taking a new camera photo and selecting one from gallery storage.
     */
    private fun showImageSourceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.receipt_image_title)
            .setItems(
                arrayOf(
                    getString(R.string.take_camera_photo),
                    getString(R.string.choose_gallery_image)
                )
            ) { _, which ->
                when (which) {
                    CAMERA_OPTION_INDEX -> cameraImageLauncher.launch(null)
                    GALLERY_OPTION_INDEX -> productImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    /**
     * Copies the chosen product image into private storage and returns its file path.
     */
    private fun saveProductImage(imageUri: android.net.Uri): String {
        val imageFile = File(requireContext().filesDir, "pantry_item_${System.currentTimeMillis()}.jpg")
        requireContext().contentResolver.openInputStream(imageUri)?.use { inputStream ->
            imageFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return imageFile.absolutePath
    }

    /**
     * Saves a camera bitmap into private storage and returns its file path.
     */
    private fun saveCameraImage(bitmap: Bitmap): String {
        val imageFile = File(requireContext().filesDir, "pantry_item_${System.currentTimeMillis()}.jpg")
        FileOutputStream(imageFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, outputStream)
        }
        return imageFile.absolutePath
    }

    /**
     * Displays the chosen product image if one has been saved for this item.
     */
    private fun showProductImage(imagePath: String) {
        if (imagePath.isBlank()) {
            binding.imageviewPantryPreview.setImageDrawable(null)
            binding.layoutPantryImageEmpty.visibility = View.VISIBLE
            return
        }

        BitmapFactory.decodeFile(imagePath)?.let {
            binding.imageviewPantryPreview.setImageBitmap(it)
            binding.layoutPantryImageEmpty.visibility = View.GONE
        } ?: run {
            binding.imageviewPantryPreview.setImageDrawable(null)
            binding.layoutPantryImageEmpty.visibility = View.VISIBLE
        }
    }

    /**
     * Provides common pantry categories so users do not need to type them manually.
     */
    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.item_categories)
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner_selected,
            categories
        )
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerPantryCategory.adapter = adapter

        if (editingPantryItemId != NO_ITEM_ID) {
            val item = databaseHelper.getPantryItemById(editingPantryItemId) ?: return
            val selectedIndex = categories.indexOf(item.category).takeIf { it >= 0 }
                ?: categories.indexOf(getString(R.string.category_default))
            binding.spinnerPantryCategory.setSelection(selectedIndex)
        }
    }

    /**
     * Opens a date picker and stores expiry dates in yyyy-MM-dd format.
     */
    private fun showExpiryDatePicker() {
        val initialDate = parseExpiryDate(binding.edittextPantryExpiry.text.toString())
            ?: LocalDate.now()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                binding.edittextPantryExpiry.setText(selectedDate.toString())
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    /**
     * Parses an existing expiry date if it is already stored in the app date format.
     */
    private fun parseExpiryDate(expiryDate: String): LocalDate? {
        return try {
            LocalDate.parse(expiryDate)
        } catch (exception: DateTimeParseException) {
            null
        }
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
        val amount = binding.edittextPantryAmount.text.toString().toIntOrNull()
            ?.coerceAtLeast(1)
            ?: DEFAULT_AMOUNT
        val category = binding.spinnerPantryCategory.selectedItem as? String
            ?: getString(R.string.category_default)
        val expiryDate = binding.edittextPantryExpiry.text.toString().trim()
            .ifEmpty { getString(R.string.expiry_default) }

        if (editingPantryItemId == NO_ITEM_ID) {
            databaseHelper.insertPantryItem(name, quantity, category, expiryDate, amount, selectedImagePath)
            Snackbar.make(binding.root, R.string.pantry_item_saved, Snackbar.LENGTH_SHORT).show()
        } else {
            databaseHelper.updatePantryItem(
                editingPantryItemId,
                name,
                quantity,
                category,
                expiryDate,
                amount,
                selectedImagePath
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
        private const val DEFAULT_AMOUNT = 1
        private const val CAMERA_OPTION_INDEX = 0
        private const val GALLERY_OPTION_INDEX = 1
        private const val IMAGE_QUALITY = 90
    }
}
