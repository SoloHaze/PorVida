package com.porvida.data.repos

import com.porvida.models.User
import com.porvida.models.UsuarioDao
import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class UserRepository(private val usuarioDao: UsuarioDao) {
    fun getAllUsers(): Flow<List<User>> = usuarioDao.getAllUsers()

    suspend fun loginUser(email: String, password: String): User? {
        val user = usuarioDao.findByEmail(email) ?: return null
        val result = BCrypt.verifyer().verify(password.toCharArray(), user.password)
        return if (result.verified) user else null
    }

    suspend fun getUserById(userId: String): User? {
        return usuarioDao.getUserById(userId)
    }

    suspend fun addUser(name: String, email: String, password: String, role: String = "CLIENT", plan: String, planComment: String? = null, planValidUntil: Long = 0L): Boolean {
        return try {
            // Hash con costo 12 (ajustable). toString() incluye versión y salt.
            val hashed = BCrypt.withDefaults().hashToString(12, password.toCharArray())
            val user = User(
                id = UUID.randomUUID().toString(),
                name = name,
                email = email,
                password = hashed,
                role = role,
                plan = plan,
                planValidUntil = planValidUntil,
                createdAt = System.currentTimeMillis(),
                planComment = planComment
            )
            usuarioDao.insertUser(user)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteUser(user: User) {
        usuarioDao.deleteUser(user)
    }

    suspend fun updateUserPlan(userId: String, newPlan: String): Boolean {
        return try {
            val user = usuarioDao.getUserById(userId) ?: return false
            usuarioDao.insertUser(user.copy(plan = newPlan))
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun extendPlanValidity(userId: String, days: Int = 30): Boolean {
        return try {
            val user = usuarioDao.getUserById(userId) ?: return false
            val base = maxOf(System.currentTimeMillis(), user.planValidUntil)
            val extended = base + days * 24L * 60 * 60 * 1000
            usuarioDao.insertUser(user.copy(planValidUntil = extended))
            true
        } catch (e: Exception) {
            false
        }
    }
}