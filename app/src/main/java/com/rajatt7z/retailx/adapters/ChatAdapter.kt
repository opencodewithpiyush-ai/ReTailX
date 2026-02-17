package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rajatt7z.retailx.databinding.ItemChatMessageBinding

data class ChatMessage(
    val message: String,
    val isUser: Boolean
)

class ChatAdapter(private val messages: MutableList<ChatMessage> = mutableListOf()) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(chatMessage: ChatMessage) {
            if (chatMessage.isUser) {
                binding.cardUser.visibility = View.VISIBLE
                binding.layoutBot.visibility = View.GONE
                binding.tvUserMessage.text = chatMessage.message
            } else {
                binding.cardUser.visibility = View.GONE
                binding.layoutBot.visibility = View.VISIBLE
                binding.tvBotMessage.text = chatMessage.message
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}
