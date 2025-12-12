package com.porvida.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey
    val id: String,
    val userId: String,
    val orderId: String,
    val amount: Double,
    val paymentMethod: String, // CARD, CASH, TRANSFER
    val status: String, // PENDING, COMPLETED, FAILED, REFUNDED
    val transactionId: String? = null,
    val description: String,
    val paymentDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY paymentDate DESC")
    fun getPaymentsByUserId(userId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE orderId = :orderId")
    fun getPaymentsByOrderId(orderId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE id = :paymentId")
    suspend fun getPaymentById(paymentId: String): Payment?

    @Query("UPDATE payments SET status = :status WHERE id = :paymentId")
    suspend fun updatePaymentStatus(paymentId: String, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Query("SELECT * FROM payments WHERE userId = :userId AND status = 'COMPLETED' ORDER BY paymentDate DESC LIMIT 1")
    suspend fun getLastCompletedPayment(userId: String): Payment?
}