package com.example.petcare

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var hamburgerIcon: ImageView
    private lateinit var welcomeText: TextView
    private lateinit var buttonVetAppointment: Button
    private lateinit var buttonSearchCaregiver: Button
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // Pet Owner UI elements
    private lateinit var upcomingAppointmentCard: View
    private lateinit var upcomingAppointmentDate: TextView
    private lateinit var pendingRequestCard: View
    private lateinit var pendingRequestSummary: TextView

    // Caregiver UI elements
    private lateinit var caregiverRequestsTitle: TextView
    private lateinit var caregiverRequestsRecyclerView: RecyclerView
    private lateinit var noRequestsText: TextView
    private lateinit var acceptedRequestCard: View
    private lateinit var acceptedRequestTitle: TextView
    private lateinit var acceptedRequestDate: TextView
    private lateinit var acceptedPetType: TextView
    private lateinit var acceptedLocation: TextView
    private lateinit var acceptedOwnerContact: TextView
    private lateinit var btnMarkComplete: Button

    private var latestAppointment: VetAppointmentData? = null
    private var latestPendingRequest: CaregiverRequestData? = null
    private var loggedInUserId: String? = null
    private var loggedInUsername: String = ""
    private var requestsPending = 0

    private val LOCATION_PERMISSION_REQUEST_CODE = 101
    private val LOCATION_PERMISSION_REQUEST_CODE_SECONDARY = 1001

    private var userLocation: String = "Not Provided"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentUserRole: String = "pet_owner"

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        hamburgerIcon = findViewById(R.id.hamburger_icon)
        welcomeText = findViewById(R.id.welcome_text)
        buttonVetAppointment = findViewById(R.id.buttonVetAppointment)
        buttonSearchCaregiver = findViewById(R.id.buttonSearchCaregiver)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Initialize Pet Owner UI views
        upcomingAppointmentCard = findViewById(R.id.upcoming_appointment_card)
        upcomingAppointmentDate = findViewById(R.id.upcoming_appointment_date)
        pendingRequestCard = findViewById(R.id.pending_request_card)
        pendingRequestSummary = findViewById(R.id.pending_request_summary)

        // Initialize Caregiver UI views
        caregiverRequestsTitle = findViewById(R.id.caregiver_requests_title)
        caregiverRequestsRecyclerView = findViewById(R.id.caregiverRequestsRecyclerView)
        noRequestsText = findViewById(R.id.no_requests_text)
        acceptedRequestCard = findViewById(R.id.accepted_request_card_include)
        acceptedRequestTitle = acceptedRequestCard.findViewById(R.id.acceptedRequestTitle)
        acceptedRequestDate = acceptedRequestCard.findViewById(R.id.acceptedRequestDate)
        acceptedPetType = acceptedRequestCard.findViewById(R.id.acceptedPetType)
        acceptedLocation = acceptedRequestCard.findViewById(R.id.acceptedLocation)
        acceptedOwnerContact = acceptedRequestCard.findViewById(R.id.acceptedOwnerContact)
        btnMarkComplete = acceptedRequestCard.findViewById(R.id.btnMarkComplete)

        // Retrieve user data from SharedPreferences
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        loggedInUserId = prefs.getString("LOGGED_IN_USER_ID", null)
        loggedInUsername = prefs.getString("LOGGED_IN_USERNAME", "User") ?: "User"
        currentUserRole = prefs.getString("user_role", "pet_owner") ?: "pet_owner"
        Log.d("MainActivity", "Logged in User ID: $loggedInUserId")

        updateSwitchRoleMenuItem()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        welcomeText.text = "Welcome, $loggedInUsername"

        hamburgerIcon.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) drawerLayout.closeDrawer(GravityCompat.END)
            else drawerLayout.openDrawer(GravityCompat.END)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    startActivity(Intent(this, UserProfileActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.nav_chatbot -> {
                    startActivity(Intent(this, ChatbotActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    performLogout()
                    true
                }
                R.id.nav_switch_role -> {
                    toggleUserRole()
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                else -> false
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            refreshUI()
        }

        buttonVetAppointment.setOnClickListener { startActivity(Intent(this, VetAppointment::class.java)) }
        buttonSearchCaregiver.setOnClickListener { showCaregiverDialog() }

        if (loggedInUserId != null) {
            refreshUI()
        } else {
            upcomingAppointmentCard.visibility = View.GONE
            pendingRequestCard.visibility = View.GONE
            caregiverRequestsTitle.visibility = View.GONE
            caregiverRequestsRecyclerView.visibility = View.GONE
            noRequestsText.visibility = View.GONE
            acceptedRequestCard.visibility = View.GONE
            Log.d("MainActivity", "No user logged in, hiding appointment and request cards.")
        }

        upcomingAppointmentCard.setOnClickListener {
            Log.d("MainActivity", "Upcoming appointment card clicked.")
            latestAppointment?.let {
                Log.d("MainActivity", "latestAppointment is not null. Opening dialog...")
                showAppointmentDetailsDialog(it)
            } ?: Log.d("MainActivity", "latestAppointment is null, cannot open dialog.")
        }

        pendingRequestCard.setOnClickListener {
            Log.d("MainActivity", "Pending request card clicked.")
            latestPendingRequest?.let { showPendingRequestDetailsDialog(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (loggedInUserId != null) {
            refreshUI()
        }
    }

    private fun checkIfRefreshComplete() {
        requestsPending--
        if (requestsPending <= 0) {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun refreshUI() {
        upcomingAppointmentCard.visibility = View.GONE
        pendingRequestCard.visibility = View.GONE
        buttonVetAppointment.visibility = View.GONE
        buttonSearchCaregiver.visibility = View.GONE
        caregiverRequestsTitle.visibility = View.GONE
        caregiverRequestsRecyclerView.visibility = View.GONE
        noRequestsText.visibility = View.GONE
        acceptedRequestCard.visibility = View.GONE

        requestsPending = 0

        if (currentUserRole == "pet_owner") {
            buttonVetAppointment.visibility = View.VISIBLE
            buttonSearchCaregiver.visibility = View.VISIBLE
            requestsPending += 2
            loadLatestAppointment()
            loadAcceptedRequestForPetOwner()
        } else {
            caregiverRequestsTitle.visibility = View.VISIBLE
            caregiverRequestsRecyclerView.visibility = View.VISIBLE
            requestsPending += 2
            loadAndDisplayCaregiverRequests()
            loadAcceptedRequestForCaregiver()
        }
    }

    private fun performLogout() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    // -------------------- Firestore Methods --------------------
    private fun loadLatestAppointment() {
        loggedInUserId?.let { userId ->
            firestore.collection("vetAppointments")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "pending")
                .orderBy("appointmentDate")
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    val doc = snapshot.documents.firstOrNull()
                    if (doc != null) {
                        val appointment = doc.toObject(VetAppointmentData::class.java)?.copy(appointmentId = doc.id)
                        appointment?.let {
                            latestAppointment = it
                            upcomingAppointmentDate.text = it.appointmentDate
                            upcomingAppointmentCard.visibility = View.VISIBLE
                        }
                    } else {
                        upcomingAppointmentCard.visibility = View.GONE
                    }
                    checkIfRefreshComplete()
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error loading appointments", e)
                    upcomingAppointmentCard.visibility = View.GONE
                    checkIfRefreshComplete()
                }
        }
    }

    private fun loadLatestPendingRequestForPetOwner() {
        loggedInUserId?.let { userId ->
            firestore.collection("caregiverRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "pending")
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    val doc = snapshot.documents.firstOrNull()
                    val request = doc?.toObject(CaregiverRequestData::class.java)?.copy(requestId = doc.id)
                    if (request != null) {
                        latestPendingRequest = request
                        pendingRequestSummary.text = "Pet Type: ${request.petType},\nLocation: ${request.location}"
                        pendingRequestCard.visibility = View.VISIBLE
                    } else {
                        pendingRequestCard.visibility = View.GONE
                    }
                    checkIfRefreshComplete()
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error loading pending request", e)
                    pendingRequestCard.visibility = View.GONE
                    checkIfRefreshComplete()
                }
        }
    }

    private fun loadAcceptedRequestForPetOwner() {
        loggedInUserId?.let { userId ->
            firestore.collection("caregiverRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "accepted")
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    val doc = snapshot.documents.firstOrNull()
                    val request = doc?.toObject(CaregiverRequestData::class.java)?.copy(requestId = doc.id)
                    if (request != null) {
                        acceptedRequestTitle.text = "Request Accepted"
                        acceptedRequestDate.text = "Scheduled for: ${request.scheduledDate}"
                        acceptedPetType.text = "Pet Type: ${request.petType}"
                        acceptedLocation.text = "Location: ${request.location}"
                        acceptedOwnerContact.text = "Caregiver Contact: ${request.ownerContact}"
                        acceptedRequestCard.visibility = View.VISIBLE
                        btnMarkComplete.visibility = View.GONE

                        pendingRequestCard.visibility = View.GONE
                    } else {
                        loadLatestPendingRequestForPetOwner()
                        requestsPending--
                    }
                    checkIfRefreshComplete()
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error loading accepted request for pet owner", e)
                    loadLatestPendingRequestForPetOwner()
                    requestsPending--
                    checkIfRefreshComplete()
                }
        }
    }

    private fun loadAndDisplayCaregiverRequests() {
        firestore.collection("caregiverRequests")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                val requests = snapshot.documents.mapNotNull {
                    it.toObject(CaregiverRequestData::class.java)?.copy(requestId = it.id)
                }

                if (requests.isNotEmpty()) {
                    caregiverRequestsTitle.visibility = View.VISIBLE
                    caregiverRequestsRecyclerView.visibility = View.VISIBLE
                    noRequestsText.visibility = View.GONE

                    val adapter = CaregiverRequestAdapter(requests.toMutableList(), loggedInUserId) { request ->
                        acceptCaregiverRequest(request.requestId) { success ->
                            if (success) {
                                Toast.makeText(this, "Request accepted!", Toast.LENGTH_SHORT).show()
                                (caregiverRequestsRecyclerView.adapter as? CaregiverRequestAdapter)?.removeRequest(request.requestId)
                                if (requests.isEmpty()) {
                                    caregiverRequestsRecyclerView.visibility = View.GONE
                                    caregiverRequestsTitle.visibility = View.GONE
                                    noRequestsText.visibility = View.VISIBLE
                                }
                                refreshUI()
                            } else {
                                Toast.makeText(this, "Failed to accept request", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    caregiverRequestsRecyclerView.layoutManager = LinearLayoutManager(this)
                    caregiverRequestsRecyclerView.adapter = adapter

                } else {
                    caregiverRequestsTitle.visibility = View.GONE
                    caregiverRequestsRecyclerView.visibility = View.GONE
                    noRequestsText.visibility = View.VISIBLE
                }
                checkIfRefreshComplete()
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error loading caregiver requests", e)
                caregiverRequestsTitle.visibility = View.GONE
                caregiverRequestsRecyclerView.visibility = View.GONE
                noRequestsText.visibility = View.VISIBLE
                checkIfRefreshComplete()
            }
    }

    private fun loadAcceptedRequestForCaregiver() {
        loggedInUserId?.let { userId ->
            firestore.collection("caregiverRequests")
                .whereEqualTo("caregiverId", userId)
                .whereEqualTo("status", "accepted")
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    val doc = snapshot.documents.firstOrNull()
                    val request = doc?.toObject(CaregiverRequestData::class.java)?.copy(requestId = doc.id)
                    if (request != null) {
                        acceptedRequestTitle.text = "Request Accepted"
                        acceptedRequestDate.text = "Scheduled for: ${request.scheduledDate}"
                        acceptedPetType.text = "Pet Type: ${request.petType}"
                        acceptedLocation.text = "Location: ${request.location}"
                        acceptedOwnerContact.text = "Owner Contact: ${request.ownerContact}"
                        acceptedRequestCard.visibility = View.VISIBLE

                        btnMarkComplete.visibility = View.VISIBLE
                        setupMarkCompleteButton(request.requestId)

                        caregiverRequestsTitle.visibility = View.GONE
                        caregiverRequestsRecyclerView.visibility = View.GONE
                        noRequestsText.visibility = View.GONE
                    } else {
                        acceptedRequestCard.visibility = View.GONE
                        caregiverRequestsTitle.visibility = View.VISIBLE
                        caregiverRequestsRecyclerView.visibility = View.VISIBLE
                        noRequestsText.visibility = View.VISIBLE
                    }
                    checkIfRefreshComplete()
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error loading accepted request", e)
                    acceptedRequestCard.visibility = View.GONE
                    checkIfRefreshComplete()
                }
        }
    }

    private fun setupMarkCompleteButton(requestId: String) {
        btnMarkComplete.setOnClickListener {
            firestore.collection("caregiverRequests")
                .document(requestId)
                .update("status", "completed")
                .addOnSuccessListener {
                    Toast.makeText(this, "Request marked as complete!", Toast.LENGTH_SHORT).show()
                    acceptedRequestCard.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update request: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showAppointmentDetailsDialog(appointment: VetAppointmentData) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_appointment_details, null)
        val detailsDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                show()
            }

        dialogView.findViewById<TextView>(R.id.textPetType).text = "Pet Type: ${appointment.petType}"
        dialogView.findViewById<TextView>(R.id.textServiceType).text = "Service: ${appointment.serviceType}"
        dialogView.findViewById<TextView>(R.id.textPetCondition).text = "Condition: ${appointment.petCondition}"
        dialogView.findViewById<TextView>(R.id.textOwnerName).text = "Owner: ${appointment.firstName} ${appointment.lastName}"
        dialogView.findViewById<TextView>(R.id.textPhone).text = "Phone: ${appointment.phoneNumber}"
        dialogView.findViewById<TextView>(R.id.textDate).text = "Date: ${appointment.appointmentDate}"

        val editButton = dialogView.findViewById<Button>(R.id.buttonEditAppointment)
        val deleteButton = dialogView.findViewById<Button>(R.id.buttonDeleteAppointment)
        val completeButton = dialogView.findViewById<Button>(R.id.buttonCompleteAppointment)

        if (currentUserRole == "pet_owner") {
            editButton.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE

            // ADDED: Set click listener for Edit button
            editButton.setOnClickListener {
                detailsDialog.dismiss()
                showEditAppointmentDialog(appointment)
            }

            // ADDED: Set click listener for Delete button
            deleteButton.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete Appointment")
                    .setMessage("Are you sure you want to delete this vet appointment?")
                    .setPositiveButton("Delete") { _, _ ->
                        val dbHelper = VetAppointmentDB(this)
                        dbHelper.deleteAppointment(appointment.appointmentId) { success ->
                            if (success) {
                                Toast.makeText(this, "Appointment deleted successfully", Toast.LENGTH_SHORT).show()
                                detailsDialog.dismiss()
                                refreshUI()
                            } else {
                                Toast.makeText(this, "Failed to delete appointment", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

        } else {
            editButton.visibility = View.GONE
            deleteButton.visibility = View.GONE
        }

        completeButton.visibility = View.VISIBLE

        completeButton.setOnClickListener {
            if (currentUserRole == "pet_owner") {
                firestore.collection("vetAppointments").document(appointment.appointmentId)
                    .update("status", "completed")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Appointment marked as complete!", Toast.LENGTH_SHORT).show()
                        detailsDialog.dismiss()
                        refreshUI()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to mark as complete: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Only the pet owner can mark an appointment as complete.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditAppointmentDialog(appointment: VetAppointmentData) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_vet_appointment, null)
        val editDialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()

        val spinnerPetType = dialogView.findViewById<Spinner>(R.id.spinnerPetType)
        val editServiceType = dialogView.findViewById<EditText>(R.id.editTextServiceType)
        val editPetCondition = dialogView.findViewById<EditText>(R.id.editTextPetCondition)
        val editFirstName = dialogView.findViewById<EditText>(R.id.editTextFirstName)
        val editLastName = dialogView.findViewById<EditText>(R.id.editTextLastName)
        val editPhone = dialogView.findViewById<EditText>(R.id.editTextPhone)
        val editDate = dialogView.findViewById<EditText>(R.id.editTextDate)
        val buttonSubmit = dialogView.findViewById<Button>(R.id.buttonSubmit)

        val petTypes = listOf("Dog", "Cat", "Bird", "Rabbit", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, petTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPetType.adapter = adapter

        val petTypePosition = petTypes.indexOf(appointment.petType)
        if (petTypePosition >= 0) {
            spinnerPetType.setSelection(petTypePosition)
        }

        editServiceType.setText(appointment.serviceType)
        editPetCondition.setText(appointment.petCondition)
        editFirstName.setText(appointment.firstName)
        editLastName.setText(appointment.lastName)
        editPhone.setText(appointment.phoneNumber)
        editDate.setText(appointment.appointmentDate)

        editDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this,
                { _, year, month, day ->
                    editDate.setText("${day}/${month + 1}/$year")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        buttonSubmit.text = "Update Appointment"

        buttonSubmit.setOnClickListener {
            val updatedPetType = spinnerPetType.selectedItem?.toString() ?: ""
            val updatedServiceType = editServiceType.text.toString().trim()
            val updatedPetCondition = editPetCondition.text.toString().trim()
            val updatedFirstName = editFirstName.text.toString().trim()
            val updatedLastName = editLastName.text.toString().trim()
            val updatedPhone = editPhone.text.toString().trim()
            val updatedDate = editDate.text.toString().trim()

            if (updatedPetType.isEmpty() || updatedServiceType.isEmpty() || updatedFirstName.isEmpty() || updatedLastName.isEmpty() || updatedPhone.isEmpty() || updatedDate.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedAppointment = appointment.copy(
                petType = updatedPetType,
                serviceType = updatedServiceType,
                petCondition = updatedPetCondition,
                firstName = updatedFirstName,
                lastName = updatedLastName,
                phoneNumber = updatedPhone,
                appointmentDate = updatedDate
            )

            val dbHelper = VetAppointmentDB(this)
            dbHelper.updateAppointment(updatedAppointment) { success ->
                if (success) {
                    Toast.makeText(this, "Appointment updated successfully", Toast.LENGTH_SHORT).show()
                    editDialog.dismiss()
                    refreshUI()
                } else {
                    Toast.makeText(this, "Failed to update appointment", Toast.LENGTH_SHORT).show()
                }
            }
        }
        editDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        editDialog.show()
    }

    private fun showPendingRequestDetailsDialog(request: CaregiverRequestData) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pending_request_details, null)
        val detailsDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                show()
            }

        val textScheduledDate = dialogView.findViewById<TextView>(R.id.textScheduledDate)
        if (request.scheduledDate.isNotEmpty() && request.status == "accepted") {
            textScheduledDate.text = "Scheduled for: ${request.scheduledDate}"
            textScheduledDate.visibility = View.VISIBLE
        } else {
            textScheduledDate.visibility = View.GONE
        }

        dialogView.findViewById<TextView>(R.id.textPetType).text = "Pet Type: ${request.petType}"
        dialogView.findViewById<TextView>(R.id.textPetAge).text = "Pet Age: ${request.petAge}"
        dialogView.findViewById<TextView>(R.id.textSpecialNeeds).text = "Special Needs: ${request.specialNeeds.ifEmpty { "None" }}"
        dialogView.findViewById<TextView>(R.id.textOwnerContact).text = "Owner Contact: ${request.ownerContact}"
        dialogView.findViewById<TextView>(R.id.textLocation).text = "Location: ${request.location}"

        val editButton = dialogView.findViewById<Button>(R.id.buttonEditRequest)
        val deleteButton = dialogView.findViewById<Button>(R.id.buttonDeleteRequest)
        val acceptButton = dialogView.findViewById<Button>(R.id.buttonAcceptRequest)

        if (currentUserRole == "pet_owner") {
            editButton.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE
            acceptButton.visibility = View.GONE

            editButton.setOnClickListener {
                detailsDialog.dismiss()
                showEditRequestDialog(request)
            }

            deleteButton.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete Request")
                    .setMessage("Are you sure you want to delete this caregiver request?")
                    .setPositiveButton("Delete") { _, _ ->
                        val dbHelper = SearchCaregiverDB(this)
                        dbHelper.deleteRequest(request.requestId) { success ->
                            if (success) {
                                Toast.makeText(this, "Request deleted successfully", Toast.LENGTH_SHORT).show()
                                detailsDialog.dismiss()
                                refreshUI()
                            } else {
                                Toast.makeText(this, "Failed to delete request", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        } else {
            editButton.visibility = View.GONE
            deleteButton.visibility = View.GONE
            acceptButton.visibility = View.VISIBLE

            acceptButton.setOnClickListener {
                acceptCaregiverRequest(request.requestId) { success ->
                    if (success) {
                        Toast.makeText(this, "Request accepted!", Toast.LENGTH_SHORT).show()
                        detailsDialog.dismiss()
                        refreshUI()
                    } else {
                        Toast.makeText(this, "Failed to accept request", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun acceptCaregiverRequest(requestId: String, callback: (Boolean) -> Unit) {
        val updates = mapOf(
            "status" to "accepted",
            "caregiverId" to loggedInUserId
        )
        firestore.collection("caregiverRequests")
            .document(requestId)
            .update(updates as Map<String, Any>)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error accepting request", e)
                callback(false)
            }
    }

    private fun showEditRequestDialog(request: CaregiverRequestData) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_search_caregiver, null)
        val editDialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        dialogTitle.text = "Edit Caregiver Request"

        val editPetType = dialogView.findViewById<EditText>(R.id.PetType)
        val editPetAge = dialogView.findViewById<EditText>(R.id.PetAge)
        val editSpecialNeeds = dialogView.findViewById<EditText>(R.id.SpecialNeeds)
        val editOwnerContact = dialogView.findViewById<EditText>(R.id.OwnerContact)
        val editScheduledDate = dialogView.findViewById<EditText>(R.id.scheduledDate)
        val buttonGetLocation = dialogView.findViewById<Button>(R.id.buttonGetLocation)
        val buttonSubmit = dialogView.findViewById<Button>(R.id.buttonSubmit)

        editPetType.setText(request.petType)
        editPetAge.setText(request.petAge.toString())
        editSpecialNeeds.setText(request.specialNeeds)
        editOwnerContact.setText(request.ownerContact)
        editScheduledDate.setText(request.scheduledDate)
        userLocation = request.location

        editScheduledDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this,
                { _, year, month, day ->
                    editScheduledDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        buttonGetLocation.setOnClickListener { checkLocationPermission() }

        buttonSubmit.text = "Update Request"

        buttonSubmit.setOnClickListener {
            val petType = editPetType.text.toString().trim()
            val petAge = editPetAge.text.toString().toIntOrNull() ?: -1
            val specialNeeds = editSpecialNeeds.text.toString().trim()
            val ownerContact = editOwnerContact.text.toString().trim()
            val scheduledDate = editScheduledDate.text.toString().trim()

            if (petType.isEmpty() || petAge < 0 || ownerContact.isEmpty() || scheduledDate.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedRequest = request.copy(
                petType = petType,
                petAge = petAge,
                specialNeeds = specialNeeds,
                ownerContact = ownerContact,
                location = userLocation,
                scheduledDate = scheduledDate
            )

            val dbHelper = SearchCaregiverDB(this)
            dbHelper.updateRequest(updatedRequest) { success ->
                if (success) {
                    Toast.makeText(this, "Request updated successfully", Toast.LENGTH_SHORT).show()
                    editDialog.dismiss()
                    refreshUI()
                } else {
                    Toast.makeText(this, "Failed to update request", Toast.LENGTH_SHORT).show()
                }
            }
        }
        editDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        editDialog.show()
    }

    private fun showCaregiverDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_search_caregiver, null)
        val dialogBuilder = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        dialogTitle.text = "Request a Caregiver"

        val editPetType = dialogView.findViewById<EditText>(R.id.PetType)
        val editPetAge = dialogView.findViewById<EditText>(R.id.PetAge)
        val editSpecialNeeds = dialogView.findViewById<EditText>(R.id.SpecialNeeds)
        val editOwnerContact = dialogView.findViewById<EditText>(R.id.OwnerContact)
        val editScheduledDate = dialogView.findViewById<EditText>(R.id.scheduledDate)
        val buttonGetLocation = dialogView.findViewById<Button>(R.id.buttonGetLocation)
        val buttonSubmit = dialogView.findViewById<Button>(R.id.buttonSubmit)

        userLocation = "Not Provided"

        editScheduledDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this,
                { _, year, month, day ->
                    editScheduledDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        buttonGetLocation.setOnClickListener { checkLocationPermission() }

        buttonSubmit.setOnClickListener {
            val petType = editPetType.text.toString().trim()
            val petAge = editPetAge.text.toString().toIntOrNull() ?: -1
            val specialNeeds = editSpecialNeeds.text.toString().trim()
            val ownerContact = editOwnerContact.text.toString().trim()
            val scheduledDate = editScheduledDate.text.toString().trim()

            if (petType.isEmpty() || petAge < 0 || ownerContact.isEmpty() || scheduledDate.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (loggedInUserId == null) {
                Toast.makeText(this, "You must be logged in to submit a request.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dbHelper = SearchCaregiverDB(this)
            dbHelper.insertRequest(
                loggedInUserId!!,
                petType,
                petAge,
                specialNeeds,
                ownerContact,
                userLocation,
                scheduledDate
            ) { success ->
                if (success) {
                    Toast.makeText(this, "Request submitted", Toast.LENGTH_SHORT).show()
                    dialogBuilder.dismiss()
                    refreshUI()
                } else {
                    Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialogBuilder.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBuilder.show()
    }

    // -------------------- Location --------------------
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else getUserLocation()
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE_SECONDARY)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                userLocation = if (!addresses.isNullOrEmpty()) "${addresses[0].locality}, ${addresses[0].countryName}" else "Unknown"
                Toast.makeText(this, "Location fetched: $userLocation", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((requestCode == LOCATION_PERMISSION_REQUEST_CODE || requestCode == LOCATION_PERMISSION_REQUEST_CODE_SECONDARY)
            && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getUserLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // -------------------- Role --------------------
    private fun toggleUserRole() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val newRole = if (currentUserRole == "pet_owner") "caregiver" else "pet_owner"
        prefs.edit().putString("user_role", newRole).apply()
        currentUserRole = newRole
        Toast.makeText(this, "Switched to $newRole mode", Toast.LENGTH_SHORT).show()
        updateSwitchRoleMenuItem()
        refreshUI()
    }

    private fun updateSwitchRoleMenuItem() {
        val menu = navigationView.menu
        val switchRoleItem = menu.findItem(R.id.nav_switch_role)
        switchRoleItem.title = if (currentUserRole == "caregiver") "Switch to Pet Owner Mode" else "Switch to Caregiver Mode"
    }
}