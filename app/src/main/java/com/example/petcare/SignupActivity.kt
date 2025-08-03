package com.example.petcare

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    private lateinit var dbHelper: UserDBHelper
    private lateinit var usernameOrEmailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var signupBtn: Button
    private lateinit var goToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_page)

        dbHelper = UserDBHelper(this)

        usernameOrEmailInput = findViewById(R.id.username_or_email_input)
        passwordInput = findViewById(R.id.password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        signupBtn = findViewById(R.id.signup_btn)
        goToLogin = findViewById(R.id.go_to_login)

        goToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        signupBtn.setOnClickListener {
            val username = usernameOrEmailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            when {
                username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
                password != confirmPassword -> {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                dbHelper.isUsernameTaken(username) -> {
                    Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val success = dbHelper.insertUser(username, password)
                    if (success) {
                        Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Signup failed. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
