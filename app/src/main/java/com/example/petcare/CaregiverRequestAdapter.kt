package com.example.petcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CaregiverRequestAdapter(
    private val requests: MutableList<CaregiverRequestData>,
    private val loggedInUserId: String?, // Add this new parameter
    private val acceptClickListener: (CaregiverRequestData) -> Unit
) : RecyclerView.Adapter<CaregiverRequestAdapter.RequestViewHolder>() {

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val petType: TextView = itemView.findViewById(R.id.textPetType)
        val location: TextView = itemView.findViewById(R.id.textLocation)
        val specialNeeds: TextView = itemView.findViewById(R.id.textSpecialNeeds)
        val acceptButton: Button = itemView.findViewById(R.id.buttonAccept)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_caregiver_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        holder.petType.text = "Pet Type: ${request.petType}"
        holder.location.text = "Location: ${request.location}"

        if (request.specialNeeds.isNotEmpty()) {
            holder.specialNeeds.text = "Special Needs: ${request.specialNeeds}"
            holder.specialNeeds.visibility = View.VISIBLE
        } else {
            holder.specialNeeds.visibility = View.GONE
        }

        // Check if the request was made by the current user
        if (request.userId == loggedInUserId) {
            holder.acceptButton.isEnabled = false
            holder.acceptButton.text = "Your Request"
            holder.acceptButton.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray))
        } else {
            holder.acceptButton.isEnabled = true
            holder.acceptButton.text = "Accept Request"
            holder.acceptButton.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.green_accent))
            holder.acceptButton.setOnClickListener {
                acceptClickListener(request)
            }
        }
    }

    override fun getItemCount() = requests.size

    fun updateRequests(newRequests: List<CaregiverRequestData>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
    }

    fun removeRequest(requestId: String) {
        val index = requests.indexOfFirst { it.requestId == requestId }
        if (index != -1) {
            requests.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}