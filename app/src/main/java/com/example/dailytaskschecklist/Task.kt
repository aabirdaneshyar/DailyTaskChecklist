package com.example.dailytaskschecklist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val numberOfTablets: String,
    val times: String, // Stored as comma-separated values for reference
    val isTakenBreakfast: Boolean = false,
    val isTakenLunch: Boolean = false,
    val isTakenDinner: Boolean = false,
    val taskTaken: Boolean = false // Set to true when saved for the day
)
