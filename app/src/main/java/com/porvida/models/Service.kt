package com.porvida.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "services")
data class Service(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val duration: Int, // duración en minutos
    val isActive: Boolean = true,
    val category: String, // BASIC, PREMIUM, VIP
    val features: String, // JSON string con características
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services WHERE isActive = 1")
    fun getActiveServices(): Flow<List<Service>>

    @Query("SELECT * FROM services")
    fun getAllServices(): Flow<List<Service>>

    @Query("SELECT * FROM services WHERE id = :serviceId")
    suspend fun getServiceById(serviceId: String): Service?

    @Query("SELECT * FROM services WHERE category = :category AND isActive = 1")
    fun getServicesByCategory(category: String): Flow<List<Service>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: Service)

    @Query("UPDATE services SET isActive = :isActive WHERE id = :serviceId")
    suspend fun updateServiceStatus(serviceId: String, isActive: Boolean)

    @Delete
    suspend fun deleteService(service: Service)
}