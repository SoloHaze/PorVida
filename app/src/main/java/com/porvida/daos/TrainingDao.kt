package com.porvida.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.porvida.models.MuscleGroup
import com.porvida.models.TrainingRecord

@Dao
interface TrainingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: TrainingRecord)

    @Query("SELECT * FROM TrainingRecord WHERE userId = :userId AND muscleGroup = :group ORDER BY updatedAt DESC")
    suspend fun listByGroup(userId: String, group: MuscleGroup): List<TrainingRecord>

    @Query("SELECT * FROM TrainingRecord WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun listAll(userId: String): List<TrainingRecord>

    @Query("DELETE FROM TrainingRecord WHERE id = :id")
    suspend fun deleteById(id: String)
}
