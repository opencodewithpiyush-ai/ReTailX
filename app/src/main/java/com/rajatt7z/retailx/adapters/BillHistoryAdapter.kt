package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rajatt7z.retailx.databinding.ItemBillHistoryBinding
import com.rajatt7z.retailx.models.Bill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillHistoryAdapter(
    private var bills: List<Bill>,
    private val onBillClick: (Bill) -> Unit
) : RecyclerView.Adapter<BillHistoryAdapter.BillViewHolder>() {

    inner class BillViewHolder(private val binding: ItemBillHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bill: Bill) {
            binding.tvBillId.text = "#${bill.id.takeLast(8).uppercase()}"
            val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            binding.tvDate.text = sdf.format(Date(bill.timestamp))
            binding.tvCustomer.text = "${bill.customerName} (${bill.customerPhone})"
            binding.tvAmount.text = String.format("$%.2f", bill.totalAmount)
            
            binding.root.setOnClickListener {
                onBillClick(bill)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BillViewHolder {
        val binding = ItemBillHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BillViewHolder, position: Int) {
        holder.bind(bills[position])
    }

    override fun getItemCount(): Int = bills.size

    fun updateList(newBills: List<Bill>) {
        bills = newBills
        notifyDataSetChanged()
    }
}
