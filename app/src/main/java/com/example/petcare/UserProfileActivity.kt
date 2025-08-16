package com.example.petcare

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {

    private lateinit var tvUsername: TextView // UPDATED: This now handles the editable username
    private lateinit var tvFullName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvPetName: TextView
    private lateinit var tvPetType: TextView
    private lateinit var tvPetAge: TextView
    private lateinit var tvPassword: TextView
    private lateinit var tvConfirmPassword: TextView

    private lateinit var etFullName: EditText
    private lateinit var etUsername: EditText // ADDED: New EditText for the username
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etPetName: EditText
    private lateinit var etPetType: EditText
    private lateinit var etPetAge: EditText

    private lateinit var buttonEdit: Button
    private lateinit var buttonSave: Button

    private lateinit var firestore: FirebaseFirestore

    private var loggedInUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_profile)

        firestore = FirebaseFirestore.getInstance()

        // REMOVED: tvEmail and etEmail
        tvUsername = findViewById(R.id.tvUsername)
        tvFullName = findViewById(R.id.tvFullName)
        tvPhone = findViewById(R.id.tvPhone)
        tvPetName = findViewById(R.id.tvPetName)
        tvPetType = findViewById(R.id.tvPetType)
        tvPetAge = findViewById(R.id.tvPetAge)
        tvPassword = findViewById(R.id.tvPassword)
        tvConfirmPassword = findViewById(R.id.tvConfirmPassword)

        etUsername = findViewById(R.id.etUsername) // ADDED: Initialize the new username EditText
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        etPetName = findViewById(R.id.etPetName)
        etPetType = findViewById(R.id.etPetType)
        etPetAge = findViewById(R.id.etPetAge)

        buttonEdit = findViewById(R.id.buttonEdit)
        buttonSave = findViewById(R.id.buttonSave)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        loggedInUserId = prefs.getString("LOGGED_IN_USER_ID", null)

        if (loggedInUserId == null) {
            Toast.makeText(this, "No user logged in!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadUserInfoFromFirestore()
        setEditMode(false)

        buttonEdit.setOnClickListener { setEditMode(true) }
        buttonSave.setOnClickListener { saveUserInfoToFirestore() }
    }

    private fun loadUserInfoFromFirestore() {
        if (loggedInUserId != null) {
            firestore.collection("users")
                .document(loggedInUserId!!)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val username = doc.getString("username") ?: ""
                        val fullName = doc.getString("full_name") ?: ""
                        val phone = doc.getString("phone") ?: ""
                        val petName = doc.getString("pet_name") ?: ""
                        val petType = doc.getString("pet_type") ?: ""
                        val petAge = doc.getLong("pet_age")?.toInt() ?: 0

                        // Update TextViews to show the label and data
                        tvUsername.text = if (username.isEmpty()) "Username: Not provided" else "Username: $username"
                        tvFullName.text = if (fullName.isEmpty()) "Full Name: Not provided" else "Full Name: $fullName"
                        tvPhone.text = if (phone.isEmpty()) "Phone no.: Not provided" else "Phone no.: $phone"
                        tvPetName.text = if (petName.isEmpty()) "Pet Name: Not provided" else "Pet Name: $petName"
                        tvPetType.text = if (petType.isEmpty()) "Pet Type: Not provided" else "Pet Type: $petType"
                        tvPetAge.text = if (petAge == 0) "Pet Age: Not provided" else "Pet Age: $petAge"

                        // Update EditTexts
                        etUsername.setText(username)
                        etFullName.setText(fullName)
                        etPhone.setText(phone)
                        etPetName.setText(petName)
                        etPetType.setText(petType)
                        etPetAge.setText(if (petAge == 0) "" else petAge.toString())
                    } else {
                        Toast.makeText(this, "User document not found in Firestore!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setEditMode(enabled: Boolean) {
        // UPDATED: Corrected lists to use username and remove email
        val textViews = listOf(tvUsername, tvFullName, tvPhone, tvPetName, tvPetType, tvPetAge, tvPassword, tvConfirmPassword)
        val editTexts = listOf(etUsername, etFullName, etPhone, etPassword, etConfirmPassword, etPetName, etPetType, etPetAge)

        textViews.forEach { it.visibility = if (enabled) View.GONE else View.VISIBLE }
        editTexts.forEach { it.visibility = if (enabled) View.VISIBLE else View.GONE }

        if (enabled) {
            etPassword.text.clear()
            etConfirmPassword.text.clear()
        }

        buttonEdit.visibility = if (enabled) View.GONE else View.VISIBLE
        buttonSave.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun saveUserInfoToFirestore() {
        val newUsername = etUsername.text.toString().trim() // UPDATED: Get new username
        val newFullName = etFullName.text.toString().trim()
        val newPhone = etPhone.text.toString().trim()
        val newPetName = etPetName.text.toString().trim()
        val newPetType = etPetType.text.toString().trim()
        val newPetAge = etPetAge.text.toString().trim().toIntOrNull() ?: 0

        val newPassword = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // UPDATED: Check for empty username instead of email
        if (newUsername.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "Username and phone cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (loggedInUserId == null) {
            Toast.makeText(this, "User document not found!", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if a password change is requested and validate
        if (newPassword.isNotEmpty() || confirmPassword.isNotEmpty()) {
            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return
            }
            if (newPassword.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val updates = mutableMapOf<String, Any>(
            "username" to newUsername, // UPDATED: Save the new username
            "full_name" to newFullName,
            "phone" to newPhone,
            "pet_name" to newPetName,
            "pet_type" to newPetType,
            "pet_age" to newPetAge
        )

        // Only add the password to the updates map if a new one was provided
        if (newPassword.isNotEmpty()) {
            updates["password"] = newPassword
        }

        firestore.collection("users").document(loggedInUserId!!)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                loadUserInfoFromFirestore()
                setEditMode(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}