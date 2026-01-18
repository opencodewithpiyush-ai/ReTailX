package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.models.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private var orders: List<Order>,
    private val canEdit: Boolean,
    private val onStatusChangeClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val btnChangeStatus: Button = itemView.findViewById(R.id.btnChangeStatus)

        fun bind(order: Order) {
            tvProductName.text = order.productName
            tvQuantity.text = "Qty: ${order.quantity}"
            tvStatus.text = order.status
            
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            tvDate.text = sdf.format(Date(order.timestamp))

            if (canEdit && order.status == "Pending") {
                btnChangeStatus.visibility = View.VISIBLE
                btnChangeStatus.setOnClickListener { onStatusChangeClick(order) }
            } else {
                btnChangeStatus.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
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
