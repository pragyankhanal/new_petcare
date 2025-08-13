package com.example.petcare

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

data class User(
    var id: String = "",
    var username: String = "",
    var password: String = ""
)

class UserDBHelper(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")

    // Insert new user
    fun insertUser(username: String, password: String, callback: (Boolean) -> Unit) {
        isUsernameTaken(username) { taken ->
            if (taken) {
                callback(false)
            } else {
                val docRef = usersRef.document()
                val user = User(id = docRef.id, username = username, password = password)
                docRef.set(user)
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        callback(false)
                    }
            }
        }
    }

    // UPDATED: Check user credentials and return the User object
    fun checkUserCredentials(username: String, password: String, callback: (User?) -> Unit) {
        usersRef
            .whereEqualTo("username", username)
            .whereEqualTo("password", password)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    // User found, get the first document
                    val user = result.documents[0].toObject(User::class.java)
                    callback(user)
                } else {
                    // No user found with those credentials
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(null)
            }
    }

    // Check if username exists
    fun isUsernameTaken(username: String, callback: (Boolean) -> Unit) {
        usersRef
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { result ->
                callback(!result.isEmpty)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }
}