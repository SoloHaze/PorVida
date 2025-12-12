package com.porvida.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Teacher(
    @PrimaryKey val id: String,
    val name: String,
    // Correo empresarial tipo "teacher" asignado por admin
    val teacherEmail: String,
    // Turno en horas semanales (20, 30, 40). Contrato definirá el valor.
    val weeklyHours: Int = 40,
)
