package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.models.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailedOrderAdapter(
    private var orders: List<Order>,
    private val onActionClick: (Order) -> Unit
) : RecyclerView.Adapter<DetailedOrderAdapter.DetailedViewHolder>() {

    inner class DetailedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvStatus: Chip = itemView.findViewById(R.id.tvStatus)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvSoldBy: TextView = itemView.findViewById(R.id.tvSoldBy)
        val btnAction: MaterialButton = itemView.findViewById(R.id.btnAction)

        fun bind(order: Order) {
            tvOrderId.text = "#${order.id.takeLast(8).uppercase()}"
            val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            tvDate.text = sdf.format(Date(order.timestamp))
            
            tvProductName.text = order.productName
            tvStatus.text = order.status
            tvQuantity.text = "Quantity: ${order.quantity}"
            // order.soldBy is likely an ID, in a real app we'd fetch the name or it might be stored
            tvSoldBy.text = "Sold By: ${order.soldBy}" 

            if (order.status == "Pending") {
                btnAction.visibility = View.VISIBLE
                btnAction.text = "Mark as Processed"
                btnAction.setOnClickListener { onActionClick(order) }
                tvStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light)
            } else {
                btnAction.visibility = View.GONE
                tvStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_detailed, parent, false)
        return DetailedViewHolder(view)
    }

    override fun onBindViewHolder(holder: DetailedViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    fun updateList(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
