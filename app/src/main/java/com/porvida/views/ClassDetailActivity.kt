package com.porvida.views

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.porvida.R
import com.porvida.AppDatabase
import com.porvida.data.repos.ClassRepository
import com.porvida.models.ClassType
import com.porvida.models.WodType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionId = intent.getStringExtra("sessionId") ?: return finish()
        val userId = intent.getStringExtra("userId") ?: ""
        val db = AppDatabase.getDatabase(this)
        val repo = ClassRepository(db.classDao())
        val acceptNow = intent.getBooleanExtra("acceptNow", false)
        setContent {
            val colorScheme = darkColorScheme(
                primary = Color(0xFFE53935),
                background = Color(0xFF121212),
                surface = Color(0xFF121212),
                onPrimary = Color.White,
                onBackground = Color(0xFFEDEDED),
                onSurface = Color(0xFFEDEDED)
            )
            MaterialTheme(colorScheme = colorScheme) {
                DetailScreen(sessionId, userId, repo, acceptNow)
            }
        }
    }
}

@Composable
private fun DetailScreen(sessionId: String, userId: String, repo: ClassRepository, acceptNow: Boolean) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<DetailState?>(null) }
    val ctx = LocalContext.current
    val db = com.porvida.AppDatabase.getDatabase(ctx)
    LaunchedEffect(sessionId) {
        withContext(Dispatchers.IO) {
            val s = repo.getSession(sessionId)
            if (s != null) {
                val count = repo.enrollmentCount(s.id)
                val teacher = db.teacherDao().getById(s.teacherId)?.name ?: ctx.getString(R.string.label_teacher)
                val sede = db.sedeDao().getSedeById(s.sedeId)?.name ?: ctx.getString(R.string.label_sede)
                state = DetailState(s.id, s.type, s.wod, teacher, sede, s.dateTimeMillis, s.durationMinutes, s.capacity, count)
            }
        }
    }
    // Si llegamos desde notificación con "Aceptar ahora"
    LaunchedEffect(acceptNow) {
        if (acceptNow) {
            withContext(Dispatchers.IO) {
                val s = repo.getSession(sessionId) ?: return@withContext
                repo.acceptSpot(s, userId)
            }
        }
    }
    state?.let { ui ->
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            val title = when (ui.type) {
                ClassType.CROSSFIT -> ctx.getString(R.string.class_title_crossfit)
                ClassType.WORKBODY -> ctx.getString(R.string.class_title_workbody)
                ClassType.RITMO -> ctx.getString(R.string.class_title_ritmo)
                ClassType.MACHINEFIT -> ctx.getString(R.string.class_machines_label)
            }
            Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(ctx.getString(R.string.label_teacher) + ": " + ui.teacher, color = MaterialTheme.colorScheme.onSurface)
            Text(ctx.getString(R.string.label_sede) + ": " + ui.sede, color = MaterialTheme.colorScheme.onSurface)
            val formattedDate = remember(ui.dateTimeMillis) {
                java.text.SimpleDateFormat("EEE d MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ui.dateTimeMillis))
            }
            Text(ctx.getString(R.string.class_datetime, formattedDate), color = MaterialTheme.colorScheme.onSurface)
            Text(ctx.getString(R.string.class_duration, ui.duration), color = MaterialTheme.colorScheme.onSurface)
            Text(ctx.getString(R.string.class_capacity, ui.enrolled, ui.capacity), color = MaterialTheme.colorScheme.onSurface)
            if (ui.type == ClassType.CROSSFIT && ui.wod != null) {
                val wodText = when (ui.wod) {
                    WodType.UPPER -> "Superior"
                    WodType.LOWER -> "Inferior"
                    WodType.ALLBODY -> "Full body"
                    else -> null
                }
                wodText?.let { Text(ctx.getString(R.string.class_wod, it), color = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.height(16.dp))

            val acceptColors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // verde
            val cancelColors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)) // rojo
            val neutralColors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)) // botón negro

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(colors = acceptColors, onClick = {
                    scope.launch {
                        val session = withContext(Dispatchers.IO) { repo.getSession(ui.id) } ?: return@launch
                        val res = withContext(Dispatchers.IO) { repo.enroll(session, userId) }
                        Toast.makeText(
                            ctx,
                            res.exceptionOrNull()?.message ?: ctx.getString(R.string.toast_enroll_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) { Text(LocalContext.current.getString(R.string.class_enroll)) }

                Button(colors = cancelColors, onClick = {
                    scope.launch {
                        val session = withContext(Dispatchers.IO) { repo.getSession(ui.id) } ?: return@launch
                        val res = withContext(Dispatchers.IO) { repo.cancel(session, userId) }
                        // Intentar obtener a quién notificar ahora que se liberó un cupo
                        val next = withContext(Dispatchers.IO) { repo.notifyNextFromWaitlist(ui.id) }
                        if (next != null) {
                            com.porvida.util.NotificationUtil.showWaitlistNotification(
                                ctx,
                                session.id,
                                next.userId,
                                title = ctx.getString(R.string.notif_seat_released_title),
                                message = ctx.getString(R.string.notif_seat_released_message)
                            )
                        }
                        Toast.makeText(
                            ctx,
                            res.exceptionOrNull()?.message ?: ctx.getString(R.string.toast_cancel_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) { Text(LocalContext.current.getString(R.string.class_cancel)) }

                Button(colors = neutralColors, onClick = {
                    scope.launch { withContext(Dispatchers.IO) { repo.joinWaitlist(ui.id, userId) } }
                }) { Text(LocalContext.current.getString(R.string.class_wait)) }
            }
            Spacer(Modifier.height(12.dp))
            Button(colors = acceptColors, onClick = {
                scope.launch {
                    val session = withContext(Dispatchers.IO) { repo.getSession(ui.id) } ?: return@launch
                    val res = withContext(Dispatchers.IO) { repo.acceptSpot(session, userId) }
                    Toast.makeText(
                        ctx,
                        res.exceptionOrNull()?.message ?: ctx.getString(R.string.toast_accept_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }) { Text(LocalContext.current.getString(R.string.class_accept_spot)) }
        }
    }
}

private data class DetailState(
    val id: String,
    val type: ClassType,
    val wod: WodType?,
    val teacher: String,
    val sede: String,
    val dateTimeMillis: Long,
    val duration: Int,
    val capacity: Int,
    val enrolled: Int,
)
