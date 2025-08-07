package com.example.petcare

import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class UserProfileActivity : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvPetName: TextView
    private lateinit var tvPetType: TextView
    private lateinit var tvPetAge: TextView

    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPetName: EditText
    private lateinit var etPetType: EditText
    private lateinit var etPetAge: EditText

    private lateinit var buttonEdit: Button
    private lateinit var buttonSave: Button

    private lateinit var userDBHelper: UserDBHelper
    private var currentUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_profile)

        // Initialize views
        tvUsername = findViewById(R.id.tvUsername)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvPetName = findViewById(R.id.tvPetName)
        tvPetType = findViewById(R.id.tvPetType)
        tvPetAge = findViewById(R.id.tvPetAge)

        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPetName = findViewById(R.id.etPetName)
        etPetType = findViewById(R.id.etPetType)
        etPetAge = findViewById(R.id.etPetAge)

        buttonEdit = findViewById(R.id.buttonEdit)
        buttonSave = findViewById(R.id.buttonSave)

        userDBHelper = UserDBHelper(this)

        // Get current logged-in username from SharedPreferences (or intent extras)
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentUsername = prefs.getString("username", null)

        if (currentUsername == null) {
            Toast.makeText(this, "No user logged in!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvUsername.text = currentUsername

        loadUserInfo()
        setEditMode(false)

        buttonEdit.setOnClickListener {
            setEditMode(true)
        }

        buttonSave.setOnClickListener {
            saveUserInfo()
        }
    }

    private fun loadUserInfo() {
        // Load user info from DB or prefs

        // For username, you have it: currentUsername

        // For email, phone, pet info - load from SharedPreferences for demo
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val email = prefs.getString("email", "") ?: ""
        val phone = prefs.getString("phone", "") ?: ""
        val petName = prefs.getString("pet_name", "") ?: ""
        val petType = prefs.getString("pet_type", "") ?: ""
        val petAge = prefs.getInt("pet_age", 0)

        // Set TextViews
        tvEmail.text = if (email.isEmpty()) "(Not provided)" else email
        tvPhone.text = if (phone.isEmpty()) "(Not provided)" else phone
        tvPetName.text = if (petName.isEmpty()) "(Not provided)" else petName
        tvPetType.text = if (petType.isEmpty()) "(Not provided)" else petType
        tvPetAge.text = if (petAge == 0) "(Not provided)" else petAge.toString()

        // Set EditTexts to same values
        etEmail.setText(email)
        etPhone.setText(phone)
        etPetName.setText(petName)
        etPetType.setText(petType)
        etPetAge.setText(if (petAge == 0) "" else petAge.toString())
    }

    private fun setEditMode(enabled: Boolean) {
        if (enabled) {
            // Show edit texts, hide text views and buttons accordingly
            tvEmail.visibility = View.GONE
            tvPhone.visibility = View.GONE
            tvPetName.visibility = View.GONE
            tvPetType.visibility = View.GONE
            tvPetAge.visibility = View.GONE

            etEmail.visibility = View.VISIBLE
            etPhone.visibility = View.VISIBLE
            etPetName.visibility = View.VISIBLE
            etPetType.visibility = View.VISIBLE
            etPetAge.visibility = View.VISIBLE

            buttonEdit.visibility = View.GONE
            buttonSave.visibility = View.VISIBLE
        } else {
            // Show text views, hide edit texts and buttons accordingly
            tvEmail.visibility = View.VISIBLE
            tvPhone.visibility = View.VISIBLE
            tvPetName.visibility = View.VISIBLE
            tvPetType.visibility = View.VISIBLE
            tvPetAge.visibility = View.VISIBLE

            etEmail.visibility = View.GONE
            etPhone.visibility = View.GONE
            etPetName.visibility = View.GONE
            etPetType.visibility = View.GONE
            etPetAge.visibility = View.GONE

            buttonEdit.visibility = View.VISIBLE
            buttonSave.visibility = View.GONE
        }
    }

    private fun saveUserInfo() {
        val newEmail = etEmail.text.toString().trim()
        val newPhone = etPhone.text.toString().trim()
        val newPetName = etPetName.text.toString().trim()
        val newPetType = etPetType.text.toString().trim()
        val newPetAgeStr = etPetAge.text.toString().trim()
        val newPetAge = newPetAgeStr.toIntOrNull() ?: 0

        // Validate minimum fields if you want
        if (newEmail.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "Email and phone cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Save into SharedPreferences (for pet and contact info)
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("email", newEmail)
            .putString("phone", newPhone)
            .putString("pet_name", newPetName)
            .putString("pet_type", newPetType)
            .putInt("pet_age", newPetAge)
            .apply()

        // Update UI
        loadUserInfo()
        setEditMode(false)

        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
    }
}
