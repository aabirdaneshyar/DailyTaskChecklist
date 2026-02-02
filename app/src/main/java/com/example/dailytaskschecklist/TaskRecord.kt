package com.example.dailytaskschecklist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_records")
data class TaskRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskName: String,
    val taskDetails: String = "",
    val date: String, // format: yyyy-MM-dd
    val timeSlot: String, // Morning, Afternoon, Evening
    val wasTaken: Boolean
)
