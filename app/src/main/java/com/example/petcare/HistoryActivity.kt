package com.example.petcare

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var vetHistoryRecyclerView: RecyclerView
    private lateinit var caregiverHistoryRecyclerView: RecyclerView
    private lateinit var vetHistoryAdapter: HistoryAdapter
    private lateinit var caregiverHistoryAdapter: HistoryAdapter
    private lateinit var vetHistoryTitle: TextView
    private lateinit var caregiverHistoryTitle: TextView
    private var currentUserRole: String = "pet_owner"
    private lateinit var currentUserId: String

    // Log tag for debugging
    private val TAG = "HistoryActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_ui)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        vetHistoryTitle = findViewById(R.id.vet_history_title)
        caregiverHistoryTitle = findViewById(R.id.caregiver_history_title)

        vetHistoryRecyclerView = findViewById(R.id.vet_appointments_recycler_view)
        vetHistoryRecyclerView.layoutManager = LinearLayoutManager(this)

        caregiverHistoryRecyclerView = findViewById(R.id.caregiver_activities_recycler_view)
        caregiverHistoryRecyclerView.layoutManager = LinearLayoutManager(this)

        // Retrieve the current user ID and role from shared preferences
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        currentUserRole = prefs.getString("user_role", "pet_owner") ?: "pet_owner"
        currentUserId = prefs.getString("LOGGED_IN_USER_ID", "") ?: ""

        // Log the retrieved user ID and role to confirm they are correct
        Log.d(TAG, "Current user role: $currentUserRole, Retrieved User ID from preferences: $currentUserId")

        // Load all relevant historical data based on the user's role
        loadHistory()
    }

    private fun loadHistory() {
        if (currentUserRole == "pet_owner") {
            // As a pet owner, load completed vet appointments and caregiver requests they made
            vetHistoryTitle.text = "Completed Vet Appointments"
            caregiverHistoryTitle.text = "Completed Caregiver Requests Sent"
            vetHistoryTitle.visibility = View.VISIBLE
            vetHistoryRecyclerView.visibility = View.VISIBLE
            caregiverHistoryTitle.visibility = View.VISIBLE
            caregiverHistoryRecyclerView.visibility = View.VISIBLE
            loadPetOwnerVetAppointments()
            loadPetOwnerCaregiverRequests()
        } else {
            // As a caregiver, load completed caregiver requests they completed
            vetHistoryTitle.visibility = View.GONE // Hide vet appointments section for caregivers
            vetHistoryRecyclerView.visibility = View.GONE
            caregiverHistoryTitle.text = "Completed Caregiver Activities"
            caregiverHistoryTitle.visibility = View.VISIBLE
            caregiverHistoryRecyclerView.visibility = View.VISIBLE
            loadCaregiverHistory()
        }
    }

    private fun loadPetOwnerVetAppointments() {
        Log.d(TAG, "Attempting to load completed vet appointments for userId: $currentUserId")
        firestore.collection("vetAppointments")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("status", "completed")
            .orderBy("appointmentDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Successfully loaded ${documents.size()} completed vet appointments.")
                val historyList = mutableListOf<HistoryItem>()
                for (document in documents) {
                    val appointment = document.toObject(VetAppointmentData::class.java)
                    val historyItem = HistoryItem(
                        title = "Vet Appointment",
                        details = "Pet: ${appointment.petType}",
                        date = appointment.appointmentDate,
                        status = "Completed"
                    )
                    historyList.add(historyItem)
                }
                vetHistoryAdapter = HistoryAdapter(historyList)
                vetHistoryRecyclerView.adapter = vetHistoryAdapter
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading pet owner vet history", e)
                Toast.makeText(this, "Failed to load vet history. See logs for details.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPetOwnerCaregiverRequests() {
        // CORRECTED: Query the "caregiverRequests" collection and order by "scheduledDate"
        Log.d(TAG, "Attempting to load completed caregiver requests from 'caregiverRequests' for userId: $currentUserId")
        firestore.collection("caregiverRequests")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("status", "completed")
            .orderBy("scheduledDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Successfully loaded ${documents.size()} completed caregiver requests sent by pet owner.")
                val historyList = mutableListOf<HistoryItem>()
                for (document in documents) {
                    val activity = document.data
                    val historyItem = HistoryItem(
                        title = "Caregiver Request",
                        details = "Pet: ${activity["petType"]}",
                        date = activity["scheduledDate"].toString(), // Use "scheduledDate"
                        status = "Completed"
                    )
                    historyList.add(historyItem)
                }
                caregiverHistoryAdapter = HistoryAdapter(historyList)
                caregiverHistoryRecyclerView.adapter = caregiverHistoryAdapter
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading pet owner caregiver history", e)
                Toast.makeText(this, "Failed to load caregiver requests. See logs for details.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCaregiverHistory() {
        // CORRECTED: Query the "caregiverRequests" collection
        Log.d(TAG, "Attempting to load completed caregiver activities from 'caregiverRequests' for caregiverId: $currentUserId")
        firestore.collection("caregiverRequests")
            .whereEqualTo("caregiverId", currentUserId)
            .whereEqualTo("status", "completed")
            .orderBy("scheduledDate", Query.Direction.DESCENDING) // Use "scheduledDate"
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Successfully loaded ${documents.size()} completed caregiver activities for caregiver.")
                val historyList = mutableListOf<HistoryItem>()
                for (document in documents) {
                    val activity = document.data
                    val historyItem = HistoryItem(
                        title = "Caregiver Activity",
                        details = "Pet: ${activity["petType"]}",
                        date = activity["scheduledDate"].toString(), // Use "scheduledDate"
                        status = "Completed"
                    )
                    historyList.add(historyItem)
                }
                caregiverHistoryAdapter = HistoryAdapter(historyList)
                caregiverHistoryRecyclerView.adapter = caregiverHistoryAdapter
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading caregiver history", e)
                Toast.makeText(this, "Failed to load caregiver history. See logs for details.", Toast.LENGTH_SHORT).show()
            }
    }
}
