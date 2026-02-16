package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rajatt7z.retailx.databinding.ItemTransactionBinding
import com.rajatt7z.retailx.models.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionsAdapter(
    private var orders: List<Order>
) : RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(order: Order) {
            binding.tvCustomerName.text = order.customerName.ifEmpty { "Customer" }
            
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            binding.tvTime.text = sdf.format(Date(order.timestamp))
            
            binding.tvAmount.text = "+$${order.totalPrice}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    fun updateList(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
