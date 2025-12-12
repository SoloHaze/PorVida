package com.porvida.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.os.Build

private const val CHANNEL_ID = "porvida_notes"

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Recordatorio de entrenamiento"
        val text = intent.getStringExtra("text") ?: "Revisa tu nota del día y objetivos"
        val userId = intent.getStringExtra("userId")
        val noteId = intent.getStringExtra("noteId")

        // Create notification channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Recordatorios PorVida", android.app.NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }

        // Deep link to NotesActivity
        val openIntent = Intent(context, com.porvida.views.NotesActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            userId?.let { putExtra("userId", it) }
            noteId?.let { putExtra("noteId", it) }
        }
        val contentPi = android.app.PendingIntent.getActivity(
            context,
            (System.currentTimeMillis()%Int.MAX_VALUE).toInt(),
            openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify((System.currentTimeMillis()%Int.MAX_VALUE).toInt(), notification)
    }
}
