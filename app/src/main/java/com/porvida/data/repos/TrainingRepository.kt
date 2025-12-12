package com.porvida.data.repos

import com.porvida.daos.TrainingDao
import com.porvida.models.MuscleGroup
import com.porvida.models.TrainingRecord

class TrainingRepository(private val dao: TrainingDao) {
    suspend fun save(record: TrainingRecord) = dao.upsert(record)
    suspend fun listAll(userId: String) = dao.listAll(userId)
    suspend fun listByGroup(userId: String, group: MuscleGroup) = dao.listByGroup(userId, group)
    suspend fun delete(id: String) = dao.deleteById(id)
}
