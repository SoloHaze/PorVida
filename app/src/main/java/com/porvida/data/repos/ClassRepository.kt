package com.porvida.data.repos

import com.porvida.daos.ClassDao
import com.porvida.models.ClassSession
import com.porvida.models.Enrollment
import com.porvida.models.WaitlistEntry
import java.util.UUID

class ClassRepository(private val classDao: ClassDao) {
    suspend fun listSessionsInRange(sedeId: String, from: Long, to: Long) =
        classDao.listSessionsInRange(sedeId, from, to)

    suspend fun getSession(sessionId: String) = classDao.getSessionById(sessionId)

    suspend fun enrollmentCount(sessionId: String) = classDao.enrollmentCount(sessionId)

    suspend fun enroll(session: ClassSession, userId: String): Result<Unit> {
        val now = System.currentTimeMillis()
        // Regla: sólo se permite alterar 30 minutos antes de la clase
        if (now > session.dateTimeMillis - 30 * 60 * 1000) {
            return Result.failure(IllegalStateException("Solo se puede inscribir/cancelar hasta 30 minutos antes"))
        }
        val count = classDao.enrollmentCount(session.id)
        return if (count < session.capacity) {
            classDao.addEnrollment(Enrollment(UUID.randomUUID().toString(), session.id, userId))
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Clase llena"))
        }
    }

    suspend fun cancel(session: ClassSession, userId: String): Result<Unit> {
        val now = System.currentTimeMillis()
        if (now > session.dateTimeMillis - 30 * 60 * 1000) {
            return Result.failure(IllegalStateException("Solo se puede cancelar hasta 30 minutos antes"))
        }
        classDao.removeEnrollment(session.id, userId)
        // Tras liberar un cupo, intentar notificar al siguiente en waitlist
        notifyNextFromWaitlist(session.id)
        return Result.success(Unit)
    }

    suspend fun joinWaitlist(sessionId: String, userId: String) {
        classDao.enqueueWaitlist(WaitlistEntry(UUID.randomUUID().toString(), sessionId, userId))
    }

    suspend fun leaveWaitlist(sessionId: String, userId: String) {
        classDao.removeFromWaitlist(sessionId, userId)
    }

    // Marca al primero aún no notificado y devuelve su entrada para que la UI dispare notificación local
    suspend fun notifyNextFromWaitlist(sessionId: String): WaitlistEntry? {
        val entry = classDao.firstWaiterToNotify(sessionId) ?: return null
        classDao.setWaitlistNotified(entry.id, System.currentTimeMillis())
        return entry
    }

    // Aceptar cupo por parte del usuario notificado dentro de los 5 minutos
    suspend fun acceptSpot(session: ClassSession, userId: String): Result<Unit> {
        val wl = classDao.getUserWaitlist(session.id, userId)
            ?: return Result.failure(IllegalStateException("No está en lista de espera"))
        val notifiedAt = wl.notifiedAt ?: return Result.failure(IllegalStateException("No ha sido notificado"))
        val within5 = System.currentTimeMillis() <= notifiedAt + 5 * 60 * 1000
        if (!within5) {
            classDao.deleteWaitlistById(wl.id)
            return Result.failure(IllegalStateException("Tiempo de aceptación expirado"))
        }
        val count = classDao.enrollmentCount(session.id)
        if (count >= session.capacity) {
            return Result.failure(IllegalStateException("Cupo ya ocupado"))
        }
        classDao.addEnrollment(Enrollment(UUID.randomUUID().toString(), session.id, userId))
        classDao.deleteWaitlistById(wl.id)
        return Result.success(Unit)
    }
}
