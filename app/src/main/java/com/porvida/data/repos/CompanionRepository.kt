package com.porvida.data.repos

import com.porvida.models.Companion
import com.porvida.models.CompanionDao
import com.porvida.models.UsuarioDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CompanionRepository(
    private val companionDao: CompanionDao,
    private val userDao: UsuarioDao
) {
    
    fun getActiveCompanionsByUserId(userId: String): Flow<List<Companion>> = 
        companionDao.getActiveCompanionsByUserId(userId)

    fun getInactiveCompanionsByUserId(userId: String): Flow<List<Companion>> =
        companionDao.getInactiveCompanionsByUserId(userId)
    
    suspend fun getActiveCompanionCount(userId: String): Int = 
        companionDao.getActiveCompanionCount(userId)
    
    suspend fun getCompanionById(companionId: String): Companion? = 
        companionDao.getCompanionById(companionId)
    
    suspend fun addCompanion(
        userId: String,
        name: String,
        lastName: String,
        birthDate: Long,
        rut: String,
        address: String,
        saveForPromo: Boolean,
        phone: String? = null,
        email: String? = null,
        emergencyContact: Boolean = false
    ): Boolean {
        return try {
            // Límite según plan: BASICO=1, BLACK=2, ULTRA=3 (consultamos el plan del usuario)
            val user = userDao.getUserById(userId)
            val maxAllowed = when (user?.plan?.uppercase()) {
                "BASICO" -> 1
                "BLACK" -> 2
                "ULTRA" -> 3
                else -> 1
            }
            val currentCount = companionDao.getActiveCompanionCount(userId)
            if (currentCount >= maxAllowed) return false
            
            val companion = Companion(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                lastName = lastName,
                relationship = "",
                birthDate = birthDate,
                rut = rut,
                address = address,
                phone = phone,
                email = email,
                emergencyContact = emergencyContact,
                saveForPromo = saveForPromo,
                isActive = true
            )
            companionDao.insertCompanion(companion)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateCompanion(companion: Companion): Boolean {
        return try {
            companionDao.insertCompanion(companion)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun deactivateCompanion(companionId: String) {
        companionDao.deactivateCompanion(companionId)
    }
    
    suspend fun activateCompanion(companionId: String) {
        companionDao.activateCompanion(companionId)
    }
    
    suspend fun deleteCompanion(companion: Companion) {
        companionDao.deleteCompanion(companion)
    }
}