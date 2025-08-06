package com.example.petcare

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SearchCaregiverDB(context: Context) :
    SQLiteOpenHelper(context, "CaregiverRequests.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE CaregiverRequests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                petType TEXT,
                petAge INTEGER,
                specialNeeds TEXT,
                ownerContact TEXT,
                location TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS CaregiverRequests")
        onCreate(db)
    }

    fun insertRequest(
        petType: String,
        petAge: Int,
        specialNeeds: String,
        ownerContact: String,
        location: String
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("petType", petType)
            put("petAge", petAge)
            put("specialNeeds", specialNeeds)
            put("ownerContact", ownerContact)
            put("location", location)
        }

        val result = db.insert("CaregiverRequests", null, values)
        db.close()
        return result != -1L
    }
}
