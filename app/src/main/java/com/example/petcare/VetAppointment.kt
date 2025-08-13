package com.example.petcare

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class VetAppointment : AppCompatActivity() {

    private lateinit var spinnerPetType: Spinner
    private lateinit var editTextServiceType: EditText
    private lateinit var editTextFirstName: EditText
    private lateinit var editTextLastName: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextDate: EditText
    private lateinit var buttonSubmit: Button

    // You no longer need a UserDBHelper instance here if you're getting the ID from SharedPreferences
    private lateinit var vetAppointmentDB: VetAppointmentDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vet_appointment)

        // Initialize views
        spinnerPetType = findViewById(R.id.spinnerPetType)
        editTextServiceType = findViewById(R.id.editTextServiceType)
        editTextFirstName = findViewById(R.id.editTextFirstName)
        editTextLastName = findViewById(R.id.editTextLastName)
        editTextPhone = findViewById(R.id.editTextPhone)
        editTextDate = findViewById(R.id.editTextDate)
        buttonSubmit = findViewById(R.id.buttonSubmit)

        // Initialize VetAppointmentDB
        vetAppointmentDB = VetAppointmentDB(this)

        // Setup pet type spinner
        val petTypes = arrayOf("Dog", "Cat")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, petTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPetType.adapter = adapter

        // Date picker
        editTextDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this,
                { _, year, month, day ->
                    editTextDate.setText("${day}/${month + 1}/$year")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Submit button
        buttonSubmit.setOnClickListener {
            // Get form data
            val petType = spinnerPetType.selectedItem.toString()
            val serviceType = editTextServiceType.text.toString().trim()
            val firstName = editTextFirstName.text.toString().trim()
            val lastName = editTextLastName.text.toString().trim()
            val phone = editTextPhone.text.toString().trim()
            val date = editTextDate.text.toString().trim()

            // Validate form fields
            if (serviceType.isEmpty() || firstName.isEmpty() ||
                lastName.isEmpty() || phone.isEmpty() || date.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- THIS IS THE KEY CHANGE ---
            // Get the current user's ID from SharedPreferences.
            // This assumes the ID was saved there upon successful login.
            val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val currentUserId = sharedPrefs.getString("LOGGED_IN_USER_ID", null)

            // Validate that a user is logged in
            if (currentUserId.isNullOrEmpty()) {
                Toast.makeText(this, "You must be logged in to book an appointment.", Toast.LENGTH_LONG).show()
                // You might want to add code here to navigate the user back to the login screen
                // or just prevent them from submitting the form.
                return@setOnClickListener
            }

            // Insert the appointment using the validated user ID from SharedPreferences
            vetAppointmentDB.insertAppointment(
                userId = currentUserId, // Use the correct, stored user ID
                petType = petType,
                serviceType = serviceType,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phone,
                appointmentDate = date
            ) { success ->
                if (success) {
                    Toast.makeText(this, "Appointment saved successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save appointment. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}