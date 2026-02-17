package com.rajatt7z.retailx.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rajatt7z.retailx.databinding.ItemCartProductBinding
import com.rajatt7z.retailx.models.CartItem

class CartAdapter(
    private var cartItems: List<CartItem>,
    private val onRemoveClick: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(private val binding: ItemCartProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItem) {
            binding.tvProductName.text = item.productName
            
            val grossPrice = item.unitPrice * item.quantity
            
            if (item.discount > 0) {
                // Show original price with strikethrough
                binding.tvPriceQuantity.text = String.format("$%.2f x %d", item.unitPrice, item.quantity)
                binding.tvPriceQuantity.paintFlags = binding.tvPriceQuantity.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                
                // Show discount info
                val discountText = when (item.discountType) {
                    "percentage" -> String.format("-%.0f%%", item.discountValue)
                    "fixed" -> String.format("-$%.2f", item.discountValue)
                    else -> ""
                }
                binding.tvTotalPrice.text = String.format("$%.2f %s", item.totalPrice, discountText)
            } else {
                binding.tvPriceQuantity.text = String.format("$%.2f x %d", item.unitPrice, item.quantity)
                binding.tvPriceQuantity.paintFlags = binding.tvPriceQuantity.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTotalPrice.text = String.format("$%.2f", item.totalPrice)
            }
            
            binding.btnRemove.setOnClickListener {
                onRemoveClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount(): Int = cartItems.size

    fun updateList(newItems: List<CartItem>) {
        cartItems = newItems
        notifyDataSetChanged()
    }
}
