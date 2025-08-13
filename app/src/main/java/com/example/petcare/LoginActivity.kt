package com.example.petcare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var goToSignup: TextView

    private lateinit var userRepo: UserDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        userRepo = UserDBHelper(this)

        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        loginBtn = findViewById(R.id.login_btn)
        goToSignup = findViewById(R.id.go_to_signup)

        goToSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        loginBtn.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // UPDATED: Firestore login check now returns a User object
            userRepo.checkUserCredentials(username, password) { user ->
                if (user != null) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                    // --- THIS IS THE KEY CHANGE ---
                    // Save the user's ID AND USERNAME to SharedPreferences
                    val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    with(sharedPrefs.edit()) {
                        putString("LOGGED_IN_USER_ID", user.id)
                        putString("LOGGED_IN_USERNAME", user.username) // This line was missing
                        apply()
                    }

                    // Optional: Log the ID and username to verify they're being saved
                    Log.d("LoginActivity", "Saved user ID: ${user.id} and Username: ${user.username}")

                    // Proceed to the main activity
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}