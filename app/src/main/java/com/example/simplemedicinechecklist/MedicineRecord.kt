package com.example.simplemedicinechecklist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicine_records")
data class MedicineRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicineName: String,
    val date: String, // format: yyyy-MM-dd
    val timeSlot: String, // Breakfast, Lunch, Dinner
    val wasTaken: Boolean
)
