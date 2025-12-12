package com.porvida.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MuscleGroup {
    // Tren superior
    HOMBRO, PECTORALES, BRAZOS, ESPALDA,
    // Tren inferior
    GLUTEO, CUADRICEPS, FEMORALES, PANTORRILLAS,
    // Abdomen
    ABDOMINALES,
    // Cardio (por ejercicio)
    CARDIO
}

@Entity
data class TrainingRecord(
    @PrimaryKey val id: String,
    val userId: String,
    val muscleGroup: MuscleGroup,
    val exerciseName: String,
    val bestWeightKg: Float? = null,
    val reps: Int? = null,
    // Para cardio/abdominales registramos tiempo total en segundos
    val timeSeconds: Int? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
