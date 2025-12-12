package com.porvida.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "service_orders")
data class ServiceOrder(
    @PrimaryKey
    val id: String,
    val userId: String,
    val serviceId: String,
    val status: String, // PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    val description: String,
    val scheduledDate: Long,
    val completedDate: Long? = null,
    val totalAmount: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ServiceOrderDao {
    @Query("SELECT * FROM service_orders WHERE userId = :userId ORDER BY createdAt DESC")
    fun getOrdersByUserId(userId: String): Flow<List<ServiceOrder>>

    @Query("SELECT * FROM service_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<ServiceOrder>>

    @Query("SELECT * FROM service_orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): ServiceOrder?

    @Query("UPDATE service_orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: ServiceOrder)

    @Delete
    suspend fun deleteOrder(order: ServiceOrder)
}