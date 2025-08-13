package com.example.petcare

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

// Data class for a caregiver request. It's now defined here.
data class CaregiverRequestData(
    var id: String = "",
    var userId: String = "", // Added userId field to link requests to a user
    var petType: String = "",
    var petAge: Int = 0,
    var specialNeeds: String = "",
    var ownerContact: String = "",
    var location: String = ""
)

class SearchCaregiverDB(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val requestsRef = db.collection("caregiverRequests")

    // Insert a new request
    fun insertRequest(
        userId: String, // Added userId as a parameter
        petType: String,
        petAge: Int,
        specialNeeds: String,
        ownerContact: String,
        location: String,
        callback: (Boolean) -> Unit
    ) {
        val docRef = requestsRef.document()
        val request = CaregiverRequestData(
            id = docRef.id,
            userId = userId,
            petType = petType,
            petAge = petAge,
            specialNeeds = specialNeeds,
            ownerContact = ownerContact,
            location = location
        )

        docRef.set(request)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    // Update an existing request
    fun updateRequest(request: CaregiverRequestData, callback: (Boolean) -> Unit) {
        if (request.id.isEmpty()) {
            callback(false)
            return
        }

        requestsRef.document(request.id)
            .set(request)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    // Delete a request
    fun deleteRequest(requestId: String, callback: (Boolean) -> Unit) {
        if (requestId.isEmpty()) {
            callback(false)
            return
        }

        requestsRef.document(requestId)
            .delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    // Fetch all requests in real-time
    fun fetchRequests(onDataChanged: (List<CaregiverRequestData>) -> Unit) {
        requestsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            val list = mutableListOf<CaregiverRequestData>()
            snapshot?.documents?.forEach { doc ->
                val request = doc.toObject(CaregiverRequestData::class.java)
                request?.let { list.add(it) }
            }
            onDataChanged(list)
        }
    }
}