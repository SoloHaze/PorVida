package com.porvida.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Note(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val details: String = "",
    val dateMillis: Long, // scheduled day (00:00 local time recommended)
    val timeOfDayMinutes: Int? = null, // optional time-of-day (e.g., 14:30 => 14*60+30)
    val muscleGroup: String = "", // optional: name from MuscleGroup
    val injuryNotes: String = "", // antecedentes de lesiones o cuidados
    val reminders: Int = 5, // number of reminders during the day (last one is end-of-day check)
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
