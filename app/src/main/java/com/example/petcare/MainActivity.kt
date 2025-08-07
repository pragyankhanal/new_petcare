package com.example.petcare

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var hamburgerIcon: ImageView
    private lateinit var welcomeText: TextView
    private lateinit var buttonVetAppointment: Button
    private lateinit var buttonSearchCaregiver: Button
    private lateinit var upcomingAppointmentCard: View
    private lateinit var upcomingAppointmentDate: TextView

    private lateinit var pendingRequestCard: View
    private lateinit var pendingRequestSummary: TextView

    private var latestAppointment: AppointmentData? = null
    private var latestPendingRequest: CaregiverRequestData? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 101
    private val LOCATION_PERMISSION_REQUEST_CODE_SECONDARY = 1001

    private var userLocation: String = "Not Provided"

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentUserRole: String = "pet_owner" // default


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
        upcomingAppointmentCard = findViewById(R.id.upcoming_appointment_card)
        upcomingAppointmentDate = findViewById(R.id.upcoming_appointment_date)
        pendingRequestCard = findViewById(R.id.pending_request_card)
        pendingRequestSummary = findViewById(R.id.pending_request_summary)
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentUserRole = prefs.getString("user_role", "pet_owner") ?: "pet_owner"
        applyUserRole()


        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Handle window insets for edge-to-edge support
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        welcomeText.text = "Welcome, Pragyan"

        hamburgerIcon.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    // Open UserProfileActivity
                    startActivity(Intent(this, UserProfileActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.END)
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

        buttonVetAppointment.setOnClickListener {
            startActivity(Intent(this, VetAppointment::class.java))
        }

        buttonSearchCaregiver.setOnClickListener {
            showCaregiverDialog()
        }

        loadLatestAppointment()
        loadLatestPendingRequest()
        updateSwitchRoleMenuItem()


        upcomingAppointmentCard.setOnClickListener {
            latestAppointment?.let { showAppointmentDetailsDialog(it) }
        }

        pendingRequestCard.setOnClickListener {
            latestPendingRequest?.let { showPendingRequestDetailsDialog(it) }
        }
    }

    private fun loadLatestAppointment() {
        val dbHelper = VetAppointmentDB(this)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM VetAppointments ORDER BY id DESC LIMIT 1", null)

        if (cursor.moveToFirst()) {
            val petType = cursor.getString(cursor.getColumnIndexOrThrow("petType"))
            val serviceType = cursor.getString(cursor.getColumnIndexOrThrow("serviceType"))
            val petCondition = cursor.getString(cursor.getColumnIndexOrThrow("petCondition"))
            val firstName = cursor.getString(cursor.getColumnIndexOrThrow("firstName"))
            val lastName = cursor.getString(cursor.getColumnIndexOrThrow("lastName"))
            val phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow("phoneNumber"))
            val appointmentDate = cursor.getString(cursor.getColumnIndexOrThrow("appointmentDate"))

            latestAppointment = AppointmentData(
                petType,
                serviceType,
                petCondition,
                "$firstName $lastName",
                phoneNumber,
                appointmentDate
            )

            upcomingAppointmentDate.text = appointmentDate
            upcomingAppointmentCard.visibility = View.VISIBLE
        } else {
            upcomingAppointmentCard.visibility = View.GONE
        }

        cursor.close()
        db.close()
    }

    private fun showAppointmentDetailsDialog(appointment: AppointmentData) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_appointment_details, null)

        dialogView.findViewById<TextView>(R.id.textPetType).text = appointment.petType
        dialogView.findViewById<TextView>(R.id.textServiceType).text = appointment.serviceType
        dialogView.findViewById<TextView>(R.id.textOwnerName).text = appointment.ownerName
        dialogView.findViewById<TextView>(R.id.textPhone).text = appointment.phone
        dialogView.findViewById<TextView>(R.id.textDate).text = appointment.appointmentDate

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun performLogout() {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit().clear().apply()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    data class AppointmentData(
        val petType: String,
        val serviceType: String,
        val petCondition: String,
        val ownerName: String,
        val phone: String,
        val appointmentDate: String
    )

    private fun showCaregiverDialog() {
        val dialogView = LayoutInflater.from(this@MainActivity).inflate(R.layout.activity_search_caregiver, null)
        val dialogBuilder = AlertDialog.Builder(this@MainActivity)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val editPetType = dialogView.findViewById<EditText>(R.id.PetType)
        val editPetAge = dialogView.findViewById<EditText>(R.id.PetAge)
        val editSpecialNeeds = dialogView.findViewById<EditText>(R.id.SpecialNeeds)
        val editOwnerContact = dialogView.findViewById<EditText>(R.id.OwnerContact)
        val buttonGetLocation = dialogView.findViewById<Button>(R.id.buttonGetLocation)
        val buttonSubmit = dialogView.findViewById<Button>(R.id.buttonSubmit)

        userLocation = "Not Provided"

        buttonGetLocation.setOnClickListener {
            checkLocationPermission()
        }

        buttonSubmit.setOnClickListener {
            val petType = editPetType.text.toString().trim()
            val petAge = editPetAge.text.toString().toIntOrNull() ?: -1
            val specialNeeds = editSpecialNeeds.text.toString().trim()
            val ownerContact = editOwnerContact.text.toString().trim()

            if (petType.isEmpty() || petAge < 0 || ownerContact.isEmpty()) {
                Toast.makeText(this@MainActivity, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dbHelper = SearchCaregiverDB(this@MainActivity)
            val success = dbHelper.insertRequest(
                petType,
                petAge,
                specialNeeds,
                ownerContact,
                userLocation
            )

            if (success) {
                Toast.makeText(this@MainActivity, "Request submitted", Toast.LENGTH_SHORT).show()
                dialogBuilder.dismiss()
            } else {
                Toast.makeText(this@MainActivity, "Failed to save data", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBuilder.show()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getUserLocation()
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE_SECONDARY)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                if (!addresses.isNullOrEmpty()) {
                    userLocation = "${addresses[0].locality}, ${addresses[0].countryName}"
                    Toast.makeText(this, "Location fetched: $userLocation", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Unable to fetch address", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((requestCode == LOCATION_PERMISSION_REQUEST_CODE || requestCode == LOCATION_PERMISSION_REQUEST_CODE_SECONDARY)
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation() // Retry location fetching after permission granted
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    data class CaregiverRequestData(
        val petType: String,
        val petAge: Int,
        val specialNeeds: String,
        val ownerContact: String,
        val location: String
    )

    private fun loadLatestPendingRequest() {
        val dbHelper = SearchCaregiverDB(this)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM CaregiverRequests ORDER BY id DESC LIMIT 1", null)

        if (cursor.moveToFirst()) {
            val petType = cursor.getString(cursor.getColumnIndexOrThrow("petType"))
            val petAge = cursor.getInt(cursor.getColumnIndexOrThrow("petAge"))
            val specialNeeds = cursor.getString(cursor.getColumnIndexOrThrow("specialNeeds"))
            val ownerContact = cursor.getString(cursor.getColumnIndexOrThrow("ownerContact"))
            val location = cursor.getString(cursor.getColumnIndexOrThrow("location"))

            latestPendingRequest = CaregiverRequestData(
                petType,
                petAge,
                specialNeeds,
                ownerContact,
                location
            )

            pendingRequestSummary.text = "Pet Type: $petType,\nLocation: $location"
            pendingRequestCard.visibility = View.VISIBLE
        } else {
            pendingRequestCard.visibility = View.GONE
        }

        cursor.close()
        db.close()
    }

    private fun showPendingRequestDetailsDialog(request: CaregiverRequestData) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pending_request_details, null)

        dialogView.findViewById<TextView>(R.id.textPetType).text = request.petType
        dialogView.findViewById<TextView>(R.id.textPetAge).text = request.petAge.toString()
        dialogView.findViewById<TextView>(R.id.textSpecialNeeds).text = request.specialNeeds.ifEmpty { "None" }
        dialogView.findViewById<TextView>(R.id.textOwnerContact).text = request.ownerContact
        dialogView.findViewById<TextView>(R.id.textLocation).text = request.location

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun toggleUserRole() {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val newRole = if (currentUserRole == "pet_owner") "caregiver" else "pet_owner"
        prefs.edit().putString("user_role", newRole).apply()
        currentUserRole = newRole
        Toast.makeText(this, "Switched to $newRole mode", Toast.LENGTH_SHORT).show()
        applyUserRole()
        updateSwitchRoleMenuItem()
    }


    private fun applyUserRole() {
        if (currentUserRole == "caregiver") {
            buttonSearchCaregiver.visibility = View.GONE
        } else {
            buttonSearchCaregiver.visibility = View.VISIBLE
        }
    }

    private fun updateSwitchRoleMenuItem() {
        val menu = navigationView.menu
        val switchRoleItem = menu.findItem(R.id.nav_switch_role)
        switchRoleItem.title = if (currentUserRole == "caregiver") {
            "Switch to Pet Owner Mode"
        } else {
            "Switch to Caregiver Mode"
        }
    }

}
