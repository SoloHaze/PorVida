package com.porvida.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.porvida.models.ChatMessage

@Dao
interface ChatDao {
    @Query("SELECT * FROM ChatMessage WHERE userId = :userId AND conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun listForUser(userId: String, conversationId: String = "default"): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(msg: ChatMessage)

    @Query("DELETE FROM ChatMessage WHERE userId = :userId AND conversationId = :conversationId")
    suspend fun clearForUser(userId: String, conversationId: String = "default")
}
