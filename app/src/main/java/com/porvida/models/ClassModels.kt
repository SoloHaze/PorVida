package com.porvida.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ClassType { CROSSFIT, WORKBODY, RITMO, MACHINEFIT }
enum class WodType { UPPER, LOWER, ALLBODY }

@Entity
data class ClassSession(
    @PrimaryKey val id: String,
    val type: ClassType,
    val teacherId: String,
    val sedeId: String,
    val dateTimeMillis: Long,
    val durationMinutes: Int = 90,
    val capacity: Int = 25,
    val wod: WodType? = null,
)

@Entity
data class Enrollment(
    @PrimaryKey val id: String,
    val sessionId: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity
data class WaitlistEntry(
    @PrimaryKey val id: String,
    val sessionId: String,
    val userId: String,
    val enqueuedAt: Long = System.currentTimeMillis(),
    // Cuando se libera un cupo, se notifica al primero y tiene 5min para aceptar
    val notifiedAt: Long? = null,
)
