package com.example.petcare

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class VetAppointment : AppCompatActivity() {

    private lateinit var spinnerPetType: Spinner
    private lateinit var editTextServiceType: EditText
    private lateinit var editTextPetCondition: EditText
    private lateinit var editTextFirstName: EditText
    private lateinit var editTextLastName: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextDate: EditText
    private lateinit var buttonSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vet_appointment)

        // Initialize views
        spinnerPetType = findViewById(R.id.spinnerPetType)
        editTextServiceType = findViewById(R.id.editTextServiceType)
        editTextPetCondition = findViewById(R.id.editTextPetCondition)
        editTextFirstName = findViewById(R.id.editTextFirstName)
        editTextLastName = findViewById(R.id.editTextLastName)
        editTextPhone = findViewById(R.id.editTextPhone)
        editTextDate = findViewById(R.id.editTextDate)
        buttonSubmit = findViewById(R.id.buttonSubmit)

        // Setup pet type spinner
        val petTypes = arrayOf("Dog", "Cat")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, petTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPetType.adapter = adapter

        // Date picker for date field
        editTextDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val dateString = "${selectedDay}/${selectedMonth + 1}/${selectedYear}"
                editTextDate.setText(dateString)
            }, year, month, day)

            datePicker.show()
        }

        // Save data on submit
        buttonSubmit.setOnClickListener {
            val petType = spinnerPetType.selectedItem.toString()
            val serviceType = editTextServiceType.text.toString().trim()
            val petCondition = editTextPetCondition.text.toString().trim()
            val firstName = editTextFirstName.text.toString().trim()
            val lastName = editTextLastName.text.toString().trim()
            val phone = editTextPhone.text.toString().trim()
            val date = editTextDate.text.toString().trim()

            if (serviceType.isEmpty() || petCondition.isEmpty() || firstName.isEmpty() ||
                lastName.isEmpty() || phone.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = VetAppointmentDB(this)
            val success = db.insertAppointment(
                petType,
                serviceType,
                petCondition,
                firstName,
                lastName,
                phone,
                date
            )

            if (success) {
                Toast.makeText(this, "Appointment saved!", Toast.LENGTH_SHORT).show()
                finish() // or clear form
            } else {
                Toast.makeText(this, "Failed to save appointment.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
