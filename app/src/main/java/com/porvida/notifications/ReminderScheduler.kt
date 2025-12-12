package com.porvida.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object ReminderScheduler {
    fun scheduleDailyReminders(
        context: Context,
        dateMillis: Long,
        title: String,
        text: String,
        times: Int,
        userId: String? = null,
        noteId: String? = null,
        firstMinutesOfDay: Int? = null // optional HH*60+MM to honor selected "Hora"
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val startCal = java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
        if (firstMinutesOfDay != null) {
            val hh = (firstMinutesOfDay / 60).coerceIn(0, 23)
            val mm = (firstMinutesOfDay % 60).coerceIn(0, 59)
            startCal.set(java.util.Calendar.HOUR_OF_DAY, hh)
            startCal.set(java.util.Calendar.MINUTE, mm)
        } else {
            startCal.set(java.util.Calendar.HOUR_OF_DAY, 8)
            startCal.set(java.util.Calendar.MINUTE, 0)
        }
        startCal.set(java.util.Calendar.SECOND, 0)
        startCal.set(java.util.Calendar.MILLISECOND, 0)
        val interval = (12 * 60 * 60 * 1000L) / (times.coerceAtLeast(1)) // spread between 8:00 and 20:00 roughly
        var whenTime = startCal.timeInMillis
        repeat(times.coerceAtMost(5)) { idx ->
            val i = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("title", title)
                putExtra("text", text)
                if (userId != null) putExtra("userId", userId)
                if (noteId != null) putExtra("noteId", noteId)
            }
            val pi = PendingIntent.getBroadcast(context, (whenTime % Int.MAX_VALUE).toInt(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenTime, pi)
            whenTime += interval
        }
        // End-of-day check at 21:00
        val end = java.util.Calendar.getInstance().apply { timeInMillis = dateMillis }
        end.set(java.util.Calendar.HOUR_OF_DAY, 21)
        end.set(java.util.Calendar.MINUTE, 0)
        end.set(java.util.Calendar.SECOND, 0)
        end.set(java.util.Calendar.MILLISECOND, 0)
        val endIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", "¿Cumpliste tu meta hoy?")
            putExtra("text", "Marca tu nota del día como cumplida si lo lograste.")
            if (userId != null) putExtra("userId", userId)
            if (noteId != null) putExtra("noteId", noteId)
        }
        val endPi = PendingIntent.getBroadcast(context, (end.timeInMillis % Int.MAX_VALUE).toInt(), endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end.timeInMillis, endPi)
    }

    fun scheduleOneReminder(
        context: Context,
        atMillis: Long,
        title: String,
        text: String,
        userId: String? = null,
        tagId: String? = null
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("text", text)
            if (userId != null) putExtra("userId", userId)
            if (tagId != null) putExtra("tagId", tagId)
        }
        val requestCode = tagId?.hashCode() ?: (atMillis % Int.MAX_VALUE).toInt()
        val pi = PendingIntent.getBroadcast(context, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
    }
}
