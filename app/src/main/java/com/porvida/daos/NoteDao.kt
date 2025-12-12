package com.porvida.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.porvida.models.Note

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM Note WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM Note WHERE userId = :userId ORDER BY dateMillis ASC, createdAt ASC")
    suspend fun listForUser(userId: String): List<Note>

    @Query("SELECT * FROM Note WHERE id = :id")
    suspend fun getById(id: String): Note?
}
