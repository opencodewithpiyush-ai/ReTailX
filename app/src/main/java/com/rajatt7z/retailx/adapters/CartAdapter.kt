package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
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
            binding.tvPriceQuantity.text = String.format("$%.2f x %d", item.unitPrice, item.quantity)
            binding.tvTotalPrice.text = String.format("$%.2f", item.totalPrice)
            
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
