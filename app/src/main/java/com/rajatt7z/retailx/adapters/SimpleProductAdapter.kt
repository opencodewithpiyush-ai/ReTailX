package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rajatt7z.retailx.R

data class SimpleProductItem(
    val name: String,
    val subtitle: String,
    val value: String,
    val rank: Int? = null
)

class SimpleProductAdapter(
    private var items: List<SimpleProductItem>
) : RecyclerView.Adapter<SimpleProductAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val tvValue: TextView = view.findViewById(R.id.tvValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvProductName.text = item.name
        holder.tvSubtitle.text = item.subtitle
        holder.tvValue.text = item.value

        if (item.rank != null) {
            holder.tvRank.visibility = View.VISIBLE
            holder.tvRank.text = item.rank.toString()
        } else {
            holder.tvRank.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<SimpleProductItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
