package com.christian.quickcart

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.christian.quickcart.databinding.ItemPantryBinding
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * RecyclerView adapter that displays saved pantry items.
 */
class PantryListAdapter(
    private var items: List<PantryItem>,
    private val onEditClicked: (PantryItem) -> Unit,
    private val onAddExpiryClicked: (PantryItem) -> Unit,
    private val onDeleteClicked: (PantryItem) -> Unit
) : RecyclerView.Adapter<PantryListAdapter.PantryItemViewHolder>() {

    /**
     * Creates a view holder for one pantry item row.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PantryItemViewHolder {
        val binding = ItemPantryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PantryItemViewHolder(binding)
    }

    /**
     * Binds the item data at the requested position.
     */
    override fun onBindViewHolder(holder: PantryItemViewHolder, position: Int) {
        holder.bind(items[position], onEditClicked, onAddExpiryClicked, onDeleteClicked)
    }

    /**
     * Returns the number of pantry items currently displayed.
     */
    override fun getItemCount(): Int = items.size

    /**
     * Replaces the visible list after adding, editing, deleting, or filtering.
     */
    fun submitItems(updatedItems: List<PantryItem>) {
        items = updatedItems
        notifyDataSetChanged()
    }

    /**
     * View holder that owns and updates one pantry item row.
     */
    class PantryItemViewHolder(
        private val binding: ItemPantryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Shows pantry item details and connects row buttons.
         */
        fun bind(
            item: PantryItem,
            onEditClicked: (PantryItem) -> Unit,
            onAddExpiryClicked: (PantryItem) -> Unit,
            onDeleteClicked: (PantryItem) -> Unit
        ) {
            val expiryState = expiryState(item)

            binding.textviewPantryItemName.text = item.name
            binding.textviewPantryItemDetails.text =
                "${item.amount} x ${item.quantity} - ${item.category} - Expires: ${item.expiryDate}"
            showProductImage(item.imagePath)
            binding.textviewPantryExpiryStatus.text = expiryState.label
            binding.textviewPantryExpiryStatus.setTextColor(Color.parseColor(expiryState.textColor))
            binding.textviewPantryExpiryStatus.setBackgroundColor(Color.parseColor(expiryState.badgeColor))
            binding.root.setBackgroundResource(expiryState.background)
            binding.buttonAddPantryExpiry.visibility =
                if (expiryState.showAddExpiry) View.VISIBLE else View.GONE
            binding.buttonEditPantryItem.setOnClickListener {
                onEditClicked(item)
            }
            binding.buttonAddPantryExpiry.setOnClickListener {
                onAddExpiryClicked(item)
            }
            binding.buttonDeletePantryItem.setOnClickListener {
                onDeleteClicked(item)
            }
        }

        /**
         * Shows a thumbnail when the pantry item has a saved product image.
         */
        private fun showProductImage(imagePath: String) {
            val bitmap = BitmapFactory.decodeFile(imagePath)

            if (bitmap == null) {
                binding.imageviewPantryItem.visibility = View.GONE
            } else {
                binding.imageviewPantryItem.visibility = View.VISIBLE
                binding.imageviewPantryItem.setImageBitmap(bitmap)
            }
        }

        /**
         * Returns a visible expiry state for fast scanning in the pantry list.
         */
        private fun expiryState(item: PantryItem): ExpiryState {
            return try {
                val expiryDate = LocalDate.parse(item.expiryDate)
                val today = LocalDate.now()

                when {
                    expiryDate.isBefore(today) -> ExpiryState(
                        label = "EXPIRED",
                        background = R.drawable.pantry_item_expired,
                        badgeColor = "#C62828",
                        textColor = "#FFFFFF",
                        showAddExpiry = false
                    )
                    !expiryDate.isAfter(today.plusDays(3)) -> ExpiryState(
                        label = "USE SOON",
                        background = R.drawable.pantry_item_soon,
                        badgeColor = "#F9A825",
                        textColor = "#3E2723",
                        showAddExpiry = false
                    )
                    else -> ExpiryState(
                        label = "OK",
                        background = R.drawable.pantry_item_normal,
                        badgeColor = "#E8F5E9",
                        textColor = "#1B5E20",
                        showAddExpiry = false
                    )
                }
            } catch (exception: DateTimeParseException) {
                ExpiryState(
                    label = "NO DATE",
                    background = R.drawable.pantry_item_normal,
                    badgeColor = "#ECEFF1",
                    textColor = "#37474F",
                    showAddExpiry = true
                )
            }
        }

        /**
         * Visual styling values for a pantry row expiry state.
         */
        private data class ExpiryState(
            val label: String,
            val background: Int,
            val badgeColor: String,
            val textColor: String,
            val showAddExpiry: Boolean
        )
    }
}
