package com.example.petcare

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

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

    private lateinit var firestore: FirebaseFirestore
    private var currentUsername: String? = null
    private var userDocId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_profile)

        firestore = FirebaseFirestore.getInstance()

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

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        currentUsername = prefs.getString("username", null)

        if (currentUsername == null) {
            Toast.makeText(this, "No user logged in!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvUsername.text = currentUsername

        loadUserInfoFromFirestore()
        setEditMode(false)

        buttonEdit.setOnClickListener { setEditMode(true) }
        buttonSave.setOnClickListener { saveUserInfoToFirestore() }
    }

    private fun loadUserInfoFromFirestore() {
        firestore.collection("users")
            .whereEqualTo("username", currentUsername)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    userDocId = doc.id
                    val email = doc.getString("email") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    val petName = doc.getString("pet_name") ?: ""
                    val petType = doc.getString("pet_type") ?: ""
                    val petAge = doc.getLong("pet_age")?.toInt() ?: 0

                    // Update TextViews
                    tvEmail.text = if (email.isEmpty()) "(Not provided)" else email
                    tvPhone.text = if (phone.isEmpty()) "(Not provided)" else phone
                    tvPetName.text = if (petName.isEmpty()) "(Not provided)" else petName
                    tvPetType.text = if (petType.isEmpty()) "(Not provided)" else petType
                    tvPetAge.text = if (petAge == 0) "(Not provided)" else petAge.toString()

                    // Update EditTexts
                    etEmail.setText(email)
                    etPhone.setText(phone)
                    etPetName.setText(petName)
                    etPetType.setText(petType)
                    etPetAge.setText(if (petAge == 0) "" else petAge.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setEditMode(enabled: Boolean) {
        val textViews = listOf(tvEmail, tvPhone, tvPetName, tvPetType, tvPetAge)
        val editTexts = listOf(etEmail, etPhone, etPetName, etPetType, etPetAge)

        textViews.forEach { it.visibility = if (enabled) View.GONE else View.VISIBLE }
        editTexts.forEach { it.visibility = if (enabled) View.VISIBLE else View.GONE }

        buttonEdit.visibility = if (enabled) View.GONE else View.VISIBLE
        buttonSave.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun saveUserInfoToFirestore() {
        val newEmail = etEmail.text.toString().trim()
        val newPhone = etPhone.text.toString().trim()
        val newPetName = etPetName.text.toString().trim()
        val newPetType = etPetType.text.toString().trim()
        val newPetAge = etPetAge.text.toString().trim().toIntOrNull() ?: 0

        if (newEmail.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "Email and phone cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (userDocId == null) {
            Toast.makeText(this, "User document not found!", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "email" to newEmail,
            "phone" to newPhone,
            "pet_name" to newPetName,
            "pet_type" to newPetType,
            "pet_age" to newPetAge
        )

        firestore.collection("users").document(userDocId!!)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                loadUserInfoFromFirestore()
                setEditMode(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
