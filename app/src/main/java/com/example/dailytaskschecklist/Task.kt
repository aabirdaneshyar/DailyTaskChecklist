package com.example.dailytaskschecklist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val numberOfTablets: String,
    val times: List<TimeSlot>, // Now uses a List for better type safety
    val priority: String = Priority.Medium.title,
    val isTakenMorning: Boolean = false,
    val isTakenAfternoon: Boolean = false,
    val isTakenEvening: Boolean = false,
    val taskTaken: Boolean = false // Set to true when saved for the day
)
