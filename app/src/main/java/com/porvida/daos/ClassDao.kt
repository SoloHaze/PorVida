package com.porvida.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.porvida.models.ClassSession
import com.porvida.models.Enrollment
import com.porvida.models.WaitlistEntry

@Dao
interface ClassDao {
    // Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ClassSession)

    @Query("SELECT * FROM ClassSession WHERE id = :id")
    suspend fun getSessionById(id: String): ClassSession?

    @Query("SELECT * FROM ClassSession WHERE sedeId = :sedeId AND dateTimeMillis BETWEEN :from AND :to ORDER BY dateTimeMillis")
    suspend fun listSessionsInRange(sedeId: String, from: Long, to: Long): List<ClassSession>

    // Enrollments
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addEnrollment(enrollment: Enrollment)

    @Query("DELETE FROM Enrollment WHERE sessionId = :sessionId AND userId = :userId")
    suspend fun removeEnrollment(sessionId: String, userId: String)

    @Query("SELECT COUNT(*) FROM Enrollment WHERE sessionId = :sessionId")
    suspend fun enrollmentCount(sessionId: String): Int

    @Query("SELECT * FROM Enrollment WHERE sessionId = :sessionId")
    suspend fun listEnrollments(sessionId: String): List<Enrollment>

    // Waitlist
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueWaitlist(entry: WaitlistEntry)

    @Query("DELETE FROM WaitlistEntry WHERE sessionId = :sessionId AND userId = :userId")
    suspend fun removeFromWaitlist(sessionId: String, userId: String)

    @Query("SELECT * FROM WaitlistEntry WHERE sessionId = :sessionId ORDER BY enqueuedAt ASC")
    suspend fun listWaitlist(sessionId: String): List<WaitlistEntry>

    @Query("SELECT * FROM WaitlistEntry WHERE sessionId = :sessionId AND notifiedAt IS NULL ORDER BY enqueuedAt ASC LIMIT 1")
    suspend fun firstWaiterToNotify(sessionId: String): WaitlistEntry?

    @Query("UPDATE WaitlistEntry SET notifiedAt = :notifiedAt WHERE id = :id")
    suspend fun setWaitlistNotified(id: String, notifiedAt: Long)

    @Query("SELECT * FROM WaitlistEntry WHERE sessionId = :sessionId AND userId = :userId LIMIT 1")
    suspend fun getUserWaitlist(sessionId: String, userId: String): WaitlistEntry?

    @Query("DELETE FROM WaitlistEntry WHERE id = :id")
    suspend fun deleteWaitlistById(id: String)
}
