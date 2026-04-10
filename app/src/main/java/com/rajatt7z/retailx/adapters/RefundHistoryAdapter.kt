package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.models.Refund
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RefundHistoryAdapter(
    private var refunds: List<Refund>,
    private val onItemClick: (Refund) -> Unit
) : RecyclerView.Adapter<RefundHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRefundId: TextView = itemView.findViewById(R.id.tvRefundId)
        val tvBillRef: TextView = itemView.findViewById(R.id.tvBillRef)
        val tvRefundAmount: TextView = itemView.findViewById(R.id.tvRefundAmount)
        val tvReason: TextView = itemView.findViewById(R.id.tvRefundReason)
        val tvDate: TextView = itemView.findViewById(R.id.tvRefundDate)
        val tvStatus: TextView = itemView.findViewById(R.id.tvRefundStatus)
        val tvProcessedBy: TextView = itemView.findViewById(R.id.tvProcessedBy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_refund_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val refund = refunds[position]

        holder.tvRefundId.text = "REF-${refund.id.takeLast(6).uppercase()}"
        holder.tvBillRef.text = "Bill: ${refund.originalBillId.takeLast(6).uppercase()}"
        holder.tvRefundAmount.text = String.format("$%.2f", refund.refundAmount)
        holder.tvReason.text = refund.reason
        holder.tvDate.text = formatDate(refund.timestamp)
        holder.tvStatus.text = refund.status
        holder.tvProcessedBy.text = "By: ${refund.processedByName}"

        holder.itemView.setOnClickListener { onItemClick(refund) }
    }

    override fun getItemCount() = refunds.size

    fun updateList(newRefunds: List<Refund>) {
        refunds = newRefunds
        notifyDataSetChanged()
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
