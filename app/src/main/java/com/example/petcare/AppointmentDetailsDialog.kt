package com.example.petcare

import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class AppointmentDetailsDialog(
    private val petType: String,
    private val serviceType: String,
    private val petCondition: String,
    private val ownerName: String,
    private val phone: String,
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_appointment_details)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Setup views
        dialog.findViewById<TextView>(R.id.textPetType).text = "Pet Type: $petType"
        dialog.findViewById<TextView>(R.id.textServiceType).text = "Service: $serviceType"
        dialog.findViewById<TextView>(R.id.textOwnerName).text = "Owner: $ownerName"
        dialog.findViewById<TextView>(R.id.textPhone).text = "Phone: $phone"

        return dialog
    }
}