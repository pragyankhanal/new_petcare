package com.example.petcare

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

// Data class for a Vet Appointment. This must be consistent across your app.
data class VetAppointmentData(
    val appointmentId: String = "",
    val userId: String = "",
    val petType: String = "",
    val serviceType: String = "",
    val petCondition: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val appointmentDate: String = ""
)

class VetAppointmentDB(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val appointmentsRef = db.collection("vetAppointments")

    // Note: The userDBHelper is not used in the provided functions,
    // so it can be removed to clean up the code.
    // private val userDBHelper = UserDBHelper(context)

    // Insert a new appointment using the Firestore user ID dynamically
    fun insertAppointment(
        userId: String,  // Firestore user ID
        petType: String,
        serviceType: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        appointmentDate: String,
        callback: (Boolean) -> Unit
    ) {
        if (userId.isEmpty()) {
            Toast.makeText(context, "User ID not found", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        val docRef = appointmentsRef.document()
        val appointment = VetAppointmentData(
            appointmentId = docRef.id,
            userId = userId,
            petType = petType,
            serviceType = serviceType,
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phoneNumber,
            appointmentDate = appointmentDate
        )

        docRef.set(appointment)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    // Update an existing appointment
    // Corrected: The parameter type is now VetAppointmentData
    // Corrected: The ID reference is now 'appointment.appointmentId'
    fun updateAppointment(appointment: VetAppointmentData, callback: (Boolean) -> Unit) {
        if (appointment.appointmentId.isEmpty()) {
            callback(false)
            return
        }

        appointmentsRef.document(appointment.appointmentId)
            .set(appointment)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating appointment: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    // Delete an appointment
    fun deleteAppointment(appointmentId: String, callback: (Boolean) -> Unit) {
        if (appointmentId.isEmpty()) {
            callback(false)
            return
        }

        appointmentsRef.document(appointmentId)
            .delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    // Fetch all appointments in real-time
    fun fetchAppointments(onDataChanged: (List<VetAppointmentData>) -> Unit) {
        appointmentsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            val list = mutableListOf<VetAppointmentData>()
            snapshot?.documents?.forEach { doc ->
                val appointment = doc.toObject(VetAppointmentData::class.java)
                appointment?.let { list.add(it) }
            }
            onDataChanged(list)
        }
    }
}