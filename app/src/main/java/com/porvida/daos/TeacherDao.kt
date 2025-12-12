package com.porvida.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.porvida.models.Teacher

@Dao
interface TeacherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(teacher: Teacher)

    @Query("SELECT * FROM Teacher WHERE id = :id")
    suspend fun getById(id: String): Teacher?

    @Query("SELECT * FROM Teacher")
    suspend fun getAll(): List<Teacher>
}
