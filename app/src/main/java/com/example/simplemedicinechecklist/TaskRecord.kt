package com.example.simplemedicinechecklist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_records")
data class TaskRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskName: String,
    val date: String, // format: yyyy-MM-dd
    val timeSlot: String, // Breakfast, Lunch, Dinner
    val wasTaken: Boolean
)
