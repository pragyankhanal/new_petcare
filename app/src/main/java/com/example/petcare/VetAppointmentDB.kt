package com.example.petcare

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class VetAppointmentDB(context: Context) : SQLiteOpenHelper(context, "VetAppointments.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE VetAppointments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                petType TEXT,
                serviceType TEXT,
                petCondition TEXT,
                firstName TEXT,
                lastName TEXT,
                phoneNumber TEXT,
                appointmentDate TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS VetAppointments")
        onCreate(db)
    }

    fun insertAppointment(
        petType: String,
        serviceType: String,
        petCondition: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        appointmentDate: String
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("petType", petType)
            put("serviceType", serviceType)
            put("petCondition", petCondition)
            put("firstName", firstName)
            put("lastName", lastName)
            put("phoneNumber", phoneNumber)
            put("appointmentDate", appointmentDate)
        }

        val result = db.insert("VetAppointments", null, values)
        return result != -1L
    }
}
