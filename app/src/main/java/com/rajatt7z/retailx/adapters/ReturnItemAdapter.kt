package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.models.CartItem

data class ReturnSelection(
    val cartItem: CartItem,
    var isSelected: Boolean = false,
    var returnQuantity: Int = 0,
    val maxQuantity: Int = 0
)

class ReturnItemAdapter(
    private var items: MutableList<ReturnSelection>,
    private val currencySymbol: String = "$",
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<ReturnItemAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelectItem)
        val tvProductName: TextView = itemView.findViewById(R.id.tvReturnProductName)
        val tvUnitPrice: TextView = itemView.findViewById(R.id.tvReturnUnitPrice)
        val tvOriginalQty: TextView = itemView.findViewById(R.id.tvOriginalQty)
        val tvReturnQty: TextView = itemView.findViewById(R.id.tvReturnQty)
        val btnMinus: ImageButton = itemView.findViewById(R.id.btnMinus)
        val btnPlus: ImageButton = itemView.findViewById(R.id.btnPlus)
        val tvRefundAmount: TextView = itemView.findViewById(R.id.tvItemRefundAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_return_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val cartItem = item.cartItem

        holder.tvProductName.text = cartItem.productName
        holder.tvUnitPrice.text = String.format("%s%.2f", currencySymbol, cartItem.unitPrice)
        holder.tvOriginalQty.text = "Bought: ${cartItem.quantity}"
        holder.tvReturnQty.text = item.returnQuantity.toString()

        val refund = item.returnQuantity * cartItem.unitPrice
        holder.tvRefundAmount.text = String.format("%s%.2f", currencySymbol, refund)

        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.cbSelect.isChecked = item.isSelected
        
        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
            if (isChecked && item.returnQuantity == 0) {
                item.returnQuantity = 1
                holder.tvReturnQty.text = "1"
                val newRefund = 1 * cartItem.unitPrice
                holder.tvRefundAmount.text = String.format("%s%.2f", currencySymbol, newRefund)
            } else if (!isChecked) {
                item.returnQuantity = 0
                holder.tvReturnQty.text = "0"
                holder.tvRefundAmount.text = String.format("%s%.2f", currencySymbol, 0.0)
            }
            onSelectionChanged()
        }

        holder.btnMinus.setOnClickListener {
            if (item.returnQuantity > 0) {
                item.returnQuantity--
                item.isSelected = item.returnQuantity > 0
                holder.cbSelect.isChecked = item.isSelected
                holder.tvReturnQty.text = item.returnQuantity.toString()
                val newRefund = item.returnQuantity * cartItem.unitPrice
                holder.tvRefundAmount.text = String.format("%s%.2f", currencySymbol, newRefund)
                onSelectionChanged()
            }
        }

        holder.btnPlus.setOnClickListener {
            if (item.returnQuantity < item.maxQuantity) {
                item.returnQuantity++
                item.isSelected = true
                holder.cbSelect.isChecked = true
                holder.tvReturnQty.text = item.returnQuantity.toString()
                val newRefund = item.returnQuantity * cartItem.unitPrice
                holder.tvRefundAmount.text = String.format("%s%.2f", currencySymbol, newRefund)
                onSelectionChanged()
            }
        }
    }

    override fun getItemCount() = items.size

    fun getSelectedItems(): List<ReturnSelection> = items.filter { it.isSelected && it.returnQuantity > 0 }

    fun getTotalRefundAmount(): Double = getSelectedItems().sumOf { it.returnQuantity * it.cartItem.unitPrice }

    fun updateList(newItems: MutableList<ReturnSelection>) {
        items = newItems
        notifyDataSetChanged()
    }
}
