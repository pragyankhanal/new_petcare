package com.example.petcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// A generic data class to represent a history item, which can be either a VetAppointment or a CaregiverActivity
data class HistoryItem(
    val title: String,
    val details: String,
    val date: String,
    val status: String
)

class HistoryAdapter(private val historyList: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.history_item_title)
        val detailsTextView: TextView = itemView.findViewById(R.id.history_item_details)
        val dateTextView: TextView = itemView.findViewById(R.id.history_item_date)
        val statusTextView: TextView = itemView.findViewById(R.id.history_item_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_item, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val currentItem = historyList[position]
        holder.titleTextView.text = currentItem.title
        holder.detailsTextView.text = currentItem.details
        holder.dateTextView.text = currentItem.date
        holder.statusTextView.text = currentItem.status
    }

    override fun getItemCount() = historyList.size
}
