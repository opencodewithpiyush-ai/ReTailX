package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rajatt7z.retailx.databinding.ItemOrderCardBinding
import com.rajatt7z.retailx.models.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrdersAdapter(
    private var orders: List<Order>,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(private val binding: ItemOrderCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(order: Order) {
            binding.tvOrderId.text = "Order #${order.id.takeLast(6).uppercase()}"
            
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            binding.tvOrderDate.text = sdf.format(Date(order.timestamp))
            
            binding.chipStatus.text = order.status
            // Customize chip color based on status if needed
            
            binding.tvItems.text = "${order.quantity} Items" // Or derived if multiple items
            binding.tvTotalAmount.text = "$${order.totalPrice}"

            binding.root.setOnClickListener {
                onOrderClick(order)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    fun updateList(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
