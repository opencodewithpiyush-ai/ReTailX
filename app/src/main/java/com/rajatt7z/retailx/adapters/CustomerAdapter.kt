package com.rajatt7z.retailx.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rajatt7z.retailx.databinding.ItemCustomerBinding
import com.rajatt7z.retailx.models.Customer

class CustomerAdapter(
    private var customers: List<Customer>,
    private val onDeleteClick: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    inner class CustomerViewHolder(private val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(customer: Customer) {
            binding.tvCustomerName.text = customer.name
            binding.tvCustomerPhone.text = "Phone: ${customer.phone}"
            binding.tvCustomerStats.text = "${customer.totalOrders} orders · $${String.format("%.2f", customer.totalSpent)} spent"
            
            // Avatar letter
            val initial = customer.name.firstOrNull()?.uppercase() ?: "?"
            binding.tvAvatar.text = initial

            binding.btnDelete.setOnClickListener {
                onDeleteClick(customer)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(customers[position])
    }

    override fun getItemCount(): Int = customers.size

    fun updateList(newList: List<Customer>) {
        customers = newList
        notifyDataSetChanged()
    }
}
