package com.christian.quickcart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.christian.quickcart.databinding.ItemPantryBinding

/**
 * RecyclerView adapter that displays saved pantry items.
 */
class PantryListAdapter(
    private var items: List<PantryItem>,
    private val onEditClicked: (PantryItem) -> Unit,
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
        holder.bind(items[position], onEditClicked, onDeleteClicked)
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
            onDeleteClicked: (PantryItem) -> Unit
        ) {
            binding.textviewPantryItemName.text = item.name
            binding.textviewPantryItemDetails.text =
                "${item.quantity} - ${item.category} - Expires: ${item.expiryDate}"
            binding.buttonEditPantryItem.setOnClickListener {
                onEditClicked(item)
            }
            binding.buttonDeletePantryItem.setOnClickListener {
                onDeleteClicked(item)
            }
        }
    }
}
