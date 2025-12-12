package com.porvida.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.porvida.R
import com.porvida.views.ClassDetailActivity

object NotificationUtil {
    const val CHANNEL_ID = "porvida_waitlist_channel"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.notif_channel_desc) }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showWaitlistNotification(context: Context, sessionId: String, userId: String, title: String, message: String) {
        ensureChannel(context)
        // Acción "Aceptar ahora"
        val intent = Intent(context, ClassDetailActivity::class.java).apply {
            putExtra("sessionId", sessionId)
            putExtra("userId", userId)
            putExtra("acceptNow", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(
            context,
            (sessionId + userId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            // Usa un ícono del sistema para evitar fallas si falta un drawable personalizado
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notif_accept_now), pi)

        NotificationManagerCompat.from(context).notify(sessionId.hashCode(), builder.build())
    }
}
