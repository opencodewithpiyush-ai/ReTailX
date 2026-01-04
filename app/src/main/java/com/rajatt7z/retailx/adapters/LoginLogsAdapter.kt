package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rajatt7z.retailx.databinding.ItemLoginLogBinding
import com.rajatt7z.retailx.models.LoginLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginLogsAdapter : ListAdapter<LoginLog, LoginLogsAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLoginLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = getItem(position)
        holder.bind(log)
    }

    class LogViewHolder(private val binding: ItemLoginLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(log: LoginLog) {
            binding.tvEmail.text = log.email
            binding.tvRole.text = log.userType
            binding.tvDevice.text = "Device: ${log.deviceName}"
            
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
            val date = Date(log.timestamp)
            binding.tvTimestamp.text = sdf.format(date)
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<LoginLog>() {
        override fun areItemsTheSame(oldItem: LoginLog, newItem: LoginLog): Boolean {
            return oldItem.logId == newItem.logId
        }

        override fun areContentsTheSame(oldItem: LoginLog, newItem: LoginLog): Boolean {
            return oldItem == newItem
        }
    }
}
