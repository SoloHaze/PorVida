package com.porvida.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.porvida.models.AssistantMessage

@Dao
interface AssistantMessageDao {
    @Query("SELECT * FROM AssistantMessage WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun listForUser(userId: String): List<AssistantMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(msg: AssistantMessage)

    @Query("DELETE FROM AssistantMessage WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}
