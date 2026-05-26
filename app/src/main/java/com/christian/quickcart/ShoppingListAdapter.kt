package com.christian.quickcart

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.christian.quickcart.databinding.ItemShoppingBinding

/**
 * RecyclerView adapter that displays QuickCart shopping items.
 */
class ShoppingListAdapter(
    private var items: List<ShoppingItem>,
    private val onBoughtChanged: (ShoppingItem, Boolean) -> Unit,
    private val onEditClicked: (ShoppingItem) -> Unit,
    private val onDeleteClicked: (ShoppingItem) -> Unit
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
        holder.bind(items[position], onBoughtChanged, onEditClicked, onDeleteClicked)
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
         * Shows item text and sends checkbox changes back to the fragment.
         */
        fun bind(
            item: ShoppingItem,
            onBoughtChanged: (ShoppingItem, Boolean) -> Unit,
            onEditClicked: (ShoppingItem) -> Unit,
            onDeleteClicked: (ShoppingItem) -> Unit
        ) {
            binding.textviewItemName.text = item.name
            binding.textviewItemDetails.text = buildItemDetails(item)
            showProductImage(item.imagePath)
            binding.checkboxBought.setOnCheckedChangeListener(null)
            binding.checkboxBought.isChecked = item.isBought
            binding.checkboxBought.setOnCheckedChangeListener { _, isChecked ->
                item.isBought = isChecked
                onBoughtChanged(item, isChecked)
            }
            binding.buttonEditItem.setOnClickListener {
                onEditClicked(item)
            }
            binding.buttonDeleteItem.setOnClickListener {
                onDeleteClicked(item)
            }
        }

        /**
         * Builds the second line of text for a shopping item row.
         */
        private fun buildItemDetails(item: ShoppingItem): String {
            val mainDetails = "${item.amount} x ${item.quantity} - ${item.category} - ${item.priority} priority"
            return if (item.notes.isBlank()) mainDetails else "$mainDetails - ${item.notes}"
        }

        /**
         * Shows a thumbnail when the shopping item has a saved product image.
         */
        private fun showProductImage(imagePath: String) {
            val bitmap = BitmapFactory.decodeFile(imagePath)

            if (bitmap == null) {
                binding.imageviewShoppingItem.visibility = View.GONE
            } else {
                binding.imageviewShoppingItem.visibility = View.VISIBLE
                binding.imageviewShoppingItem.setImageBitmap(bitmap)
            }
        }
    }
}
