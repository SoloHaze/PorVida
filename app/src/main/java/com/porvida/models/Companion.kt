package com.porvida.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "companions")
data class Companion(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    // Nuevo: apellidos del acompañante
    val lastName: String,
    // Campo opcional previo mantenido por compatibilidad/lógica futura
    val relationship: String = "", // SPOUSE, CHILD, PARENT, FRIEND, etc.
    val birthDate: Long,
    // Nuevo: RUT y dirección
    val rut: String,
    val address: String,
    val phone: String? = null,
    val email: String? = null,
    val emergencyContact: Boolean = false,
    // Nuevo: guardar para promo futura
    val saveForPromo: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface CompanionDao {
    @Query("SELECT * FROM companions WHERE userId = :userId AND isActive = 1 ORDER BY name")
    fun getActiveCompanionsByUserId(userId: String): Flow<List<Companion>>

    @Query("SELECT * FROM companions WHERE userId = :userId AND isActive = 0 ORDER BY name")
    fun getInactiveCompanionsByUserId(userId: String): Flow<List<Companion>>

    @Query("SELECT COUNT(*) FROM companions WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveCompanionCount(userId: String): Int

    @Query("SELECT * FROM companions WHERE id = :companionId")
    suspend fun getCompanionById(companionId: String): Companion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanion(companion: Companion)

    @Query("UPDATE companions SET isActive = 0 WHERE id = :companionId")
    suspend fun deactivateCompanion(companionId: String)

    @Query("UPDATE companions SET isActive = 1 WHERE id = :companionId")
    suspend fun activateCompanion(companionId: String)

    @Delete
    suspend fun deleteCompanion(companion: Companion)
}