package com.example.dailytaskschecklist

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTimeSlotList(value: List<TimeSlot>): String {
        return value.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toTimeSlotList(value: String): List<TimeSlot> {
        if (value.isEmpty()) return emptyList()
        return value.split(",").map { 
            // Handle both name (from new version) and title (from legacy comma-separated string)
            try {
                TimeSlot.valueOf(it)
            } catch (e: IllegalArgumentException) {
                // Fallback to title matching for legacy data
                TimeSlot.entries.find { slot -> slot.title == it.trim() } ?: TimeSlot.Morning
            }
        }
    }
}
