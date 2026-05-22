package com.christian.quickcart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.christian.quickcart.databinding.ItemShoppingBinding

/**
 * RecyclerView adapter that displays QuickCart shopping items.
 */
class ShoppingListAdapter(
    private var items: List<ShoppingItem>
) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingItemViewHolder>() {

    /**
     * Creates a view holder for one shopping item row.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val binding = ItemShoppingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ShoppingItemViewHolder(binding)
    }

    /**
     * Binds the item data at the requested position to the row controls.
     */
    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    /**
     * Returns the number of shopping items currently displayed.
     */
    override fun getItemCount(): Int = items.size

    /**
     * Replaces the visible list after search filtering changes.
     */
    fun submitItems(updatedItems: List<ShoppingItem>) {
        items = updatedItems
        notifyDataSetChanged()
    }

    /**
     * View holder that owns and updates the views for a single shopping item.
     */
    class ShoppingItemViewHolder(
        private val binding: ItemShoppingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Shows item text and stores the bought checkbox state.
         */
        fun bind(item: ShoppingItem) {
            binding.textviewItemName.text = item.name
            binding.textviewItemDetails.text =
                "${item.quantity} - ${item.category} - ${item.priority} priority"
            binding.checkboxBought.setOnCheckedChangeListener(null)
            binding.checkboxBought.isChecked = item.isBought
            binding.checkboxBought.setOnCheckedChangeListener { _, isChecked ->
                item.isBought = isChecked
            }
        }
    }
}
