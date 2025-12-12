package com.porvida.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sedes")
data class Sede(
    @PrimaryKey
    val id: String,
    val name: String,
    val address: String,
    val city: String,
    val phone: String,
    val email: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val workingHours: String, // JSON string con horarios
    val services: String, // JSON string con servicios disponibles
    val capacity: Int,
    val amenities: String, // JSON string con amenidades
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SedeDao {
    @Query("SELECT * FROM sedes WHERE isActive = 1 ORDER BY name")
    fun getActiveSedes(): Flow<List<Sede>>

    @Query("SELECT * FROM sedes")
    fun getAllSedes(): Flow<List<Sede>>

    @Query("SELECT * FROM sedes WHERE id = :sedeId")
    suspend fun getSedeById(sedeId: String): Sede?

    @Query("SELECT * FROM sedes WHERE city = :city AND isActive = 1")
    fun getSedesByCity(city: String): Flow<List<Sede>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSede(sede: Sede)

    @Query("UPDATE sedes SET isActive = :isActive WHERE id = :sedeId")
    suspend fun updateSedeStatus(sedeId: String, isActive: Boolean)

    @Delete
    suspend fun deleteSede(sede: Sede)
}