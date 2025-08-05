package com.example.petcare

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var hamburgerIcon: ImageView
    private lateinit var welcomeText: TextView
    private lateinit var buttonVetAppointment: Button
    private lateinit var buttonSearchCaregiver: Button
    private lateinit var upcomingAppointmentCard: View
    private lateinit var upcomingAppointmentDate: TextView

    private var latestAppointment: AppointmentData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        hamburgerIcon = findViewById(R.id.hamburger_icon)
        welcomeText = findViewById(R.id.welcome_text)
        buttonVetAppointment = findViewById(R.id.buttonVetAppointment)
        buttonSearchCaregiver = findViewById(R.id.buttonSearchCaregiver)
        upcomingAppointmentCard = findViewById(R.id.upcoming_appointment_card)
        upcomingAppointmentDate = findViewById(R.id.upcoming_appointment_date)

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
                    Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.nav_logout -> {
                    performLogout()
                    true
                }
                else -> false
            }
        }

        buttonVetAppointment.setOnClickListener {
            startActivity(Intent(this, VetAppointment::class.java))
        }

        buttonSearchCaregiver.setOnClickListener {
            Toast.makeText(this, "Search Caregiver clicked", Toast.LENGTH_SHORT).show()
        }

        // Load the most recent appointment and display it
        loadLatestAppointment()

        // Set up the click to show full appointment details
        upcomingAppointmentCard.setOnClickListener {
            latestAppointment?.let { showAppointmentDetailsDialog(it) }
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
            // Hide the card if there is no data
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
}
