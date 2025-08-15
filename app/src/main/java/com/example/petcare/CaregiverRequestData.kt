package com.example.petcare

import com.google.firebase.firestore.DocumentId

data class CaregiverRequestData(
    @DocumentId
    val requestId: String = "",
    val userId: String = "",
    val petType: String = "",
    val petAge: Int = 0,
    val specialNeeds: String = "",
    val ownerContact: String = "",
    val location: String = "",
    val status: String = "", // "pending" or "accepted"
    val caregiverId: String? = null,
    val scheduledDate: String = "" // NEW FIELD
)