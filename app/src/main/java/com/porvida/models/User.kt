package com.porvida.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val role: String = "CLIENT", // CLIENT, ADMIN, TECHNICIAN
    val plan: String, // BASICO, BLACK, ULTRA
    val planValidUntil: Long = 0L, // epoch millis; 0 = sin establecer
    val createdAt: Long = System.currentTimeMillis(),
    val planComment: String? = null
)

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("UPDATE users SET plan = :plan, planComment = :planComment WHERE id = :userId")
    suspend fun updateUserPlanWithComment(userId: String, plan: String, planComment: String?)

}