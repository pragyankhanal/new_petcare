package com.example.petcare

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class SearchCaregiverDB(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val requestsCollection = firestore.collection("caregiverRequests")

    // In SearchCaregiverDB.kt, find this function and update it
    fun insertRequest(
        userId: String,
        petType: String,
        petAge: Int,
        specialNeeds: String,
        ownerContact: String,
        location: String,
        scheduledDate: String, // NEW: Add this parameter
        callback: (Boolean) -> Unit
    ) {
        val request = CaregiverRequestData(
            userId = userId,
            petType = petType,
            petAge = petAge,
            specialNeeds = specialNeeds,
            ownerContact = ownerContact,
            location = location,
            status = "pending",
            caregiverId = null,
            scheduledDate = scheduledDate // NEW: Set the scheduledDate
        )

        requestsCollection.add(request)
            .addOnSuccessListener {
                Log.d("SearchCaregiverDB", "Request saved with ID: ${it.id}")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("SearchCaregiverDB", "Error adding document", e)
                Toast.makeText(context, "Error submitting request: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    fun updateRequest(request: CaregiverRequestData, callback: (Boolean) -> Unit) {
        requestsCollection.document(request.requestId)
            .set(request)
            .addOnSuccessListener {
                Log.d("SearchCaregiverDB", "Request updated with ID: ${request.requestId}")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("SearchCaregiverDB", "Error updating request", e)
                Toast.makeText(context, "Error updating request: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    fun deleteRequest(requestId: String, callback: (Boolean) -> Unit) {
        requestsCollection.document(requestId)
            .delete()
            .addOnSuccessListener {
                Log.d("SearchCaregiverDB", "Request deleted with ID: $requestId")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e("SearchCaregiverDB", "Error deleting request", e)
                Toast.makeText(context, "Error deleting request: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }
}