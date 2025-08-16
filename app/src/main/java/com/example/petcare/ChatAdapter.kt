package com.example.petcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petcare.data.ChatMessage

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userMessage: TextView = itemView.findViewById(R.id.textUserMessage)
        val aiMessage: TextView = itemView.findViewById(R.id.textAiMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        if (message.isUserMessage) {
            holder.userMessage.text = message.text
            holder.userMessage.visibility = View.VISIBLE
            holder.aiMessage.visibility = View.GONE
        } else {
            holder.aiMessage.text = message.text
            holder.aiMessage.visibility = View.VISIBLE
            holder.userMessage.visibility = View.GONE
        }
    }

    override fun getItemCount() = messages.size
}