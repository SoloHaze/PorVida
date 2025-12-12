package com.porvida.views

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.porvida.R
import androidx.compose.ui.unit.dp
import com.porvida.AppDatabase
import com.porvida.data.repos.ClassRepository
import com.porvida.models.ClassSession
import com.porvida.models.ClassType
import com.porvida.models.WodType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("userId") ?: ""
        val sedeId = intent.getStringExtra("sedeId") ?: ""
        val db = AppDatabase.getDatabase(this)
        val repo = ClassRepository(db.classDao())
        setContent {
            val colorScheme = darkColorScheme(
                primary = Color(0xFFE53935), // rojo
                background = Color(0xFF121212),
                surface = Color(0xFF121212),
                onPrimary = Color.White,
                onBackground = Color(0xFFEDEDED),
                onSurface = Color(0xFFEDEDED)
            )
            MaterialTheme(colorScheme = colorScheme) {
                ClassesScreen(userId, sedeId, repo)
            }
        }
    }
}

@Composable
private fun ClassesScreen(userId: String, sedeId: String, repo: ClassRepository) {
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf(listOf<ClassSession>()) }
    val ctx = LocalContext.current
    val db = remember(ctx) { AppDatabase.getDatabase(ctx) }
    LaunchedEffect(sedeId) {
        val cal = java.util.Calendar.getInstance()
        // Start from today at 06:00 local time
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 6)
        val start = cal.timeInMillis
        val end = start + 7L * 24 * 60 * 60 * 1000
        val loaded = withContext(Dispatchers.IO) { repo.listSessionsInRange(sedeId, start, end) }
        if (loaded.isEmpty()) {
            withContext(Dispatchers.IO) {
                // Seed teacher
                val t = com.porvida.models.Teacher(id = "t_demo", name = "Prof. Demo", teacherEmail = "prof.demo@porvida.cl", weeklyHours = 40)
                db.teacherDao().upsert(t)

                for (d in 0 until 7) {
                    // Slots: 06:00 (Crossfit), 12:00 (Ritmo), 18:00 (MachineFit)
                    // Crossfit WOD rotates by day: UPPER, LOWER, ALLBODY
                    val dayBase = start + d * 24L * 60 * 60 * 1000
                    val wod = when (d % 3) {
                        0 -> WodType.UPPER
                        1 -> WodType.LOWER
                        else -> WodType.ALLBODY
                    }
                    val cf = ClassSession(
                        id = java.util.UUID.randomUUID().toString(),
                        type = ClassType.CROSSFIT,
                        teacherId = t.id,
                        sedeId = sedeId,
                        dateTimeMillis = dayBase + 0L * 60 * 60 * 1000, // 06:00
                        durationMinutes = 90,
                        capacity = 25,
                        wod = wod,
                    )
                    val ritmo = ClassSession(
                        id = java.util.UUID.randomUUID().toString(),
                        type = ClassType.RITMO,
                        teacherId = t.id,
                        sedeId = sedeId,
                        dateTimeMillis = dayBase + 6L * 60 * 60 * 1000, // 12:00
                        durationMinutes = 60,
                        capacity = 20,
                    )
                    val machine = ClassSession(
                        id = java.util.UUID.randomUUID().toString(),
                        type = ClassType.MACHINEFIT,
                        teacherId = t.id,
                        sedeId = sedeId,
                        dateTimeMillis = dayBase + 12L * 60 * 60 * 1000, // 18:00
                        durationMinutes = 60,
                        capacity = 20,
                    )
                    db.classDao().upsertSession(cf)
                    db.classDao().upsertSession(ritmo)
                    db.classDao().upsertSession(machine)
                }
            }
        }
        sessions = withContext(Dispatchers.IO) { repo.listSessionsInRange(sedeId, start, end) }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
    Text(ctx.getString(R.string.classes_title), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sessions) { s ->
                ClassItem(s) { sess ->
                    val act = ctx as AppCompatActivity
                    val i = android.content.Intent(act, ClassDetailActivity::class.java)
                    i.putExtra("sessionId", sess.id)
                    i.putExtra("userId", userId)
                    act.startActivity(i)
                }
            }
        }
    }
}

@Composable
private fun ClassItem(session: ClassSession, onClick: (ClassSession) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(12.dp)
            .clickable { onClick(session) }
    ) {
        val ctx = LocalContext.current
        val title = when (session.type) {
            ClassType.CROSSFIT -> ctx.getString(R.string.class_title_crossfit)
            ClassType.WORKBODY -> ctx.getString(R.string.class_title_workbody)
            ClassType.RITMO -> ctx.getString(R.string.class_title_ritmo)
            ClassType.MACHINEFIT -> ctx.getString(R.string.class_machines_label)
        }
        Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        val formattedDate = remember(session.dateTimeMillis) {
            java.text.SimpleDateFormat("EEE d MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(session.dateTimeMillis))
        }
        Text(ctx.getString(R.string.class_datetime, formattedDate), color = MaterialTheme.colorScheme.onSurface)
        Text(ctx.getString(R.string.class_duration, session.durationMinutes), color = MaterialTheme.colorScheme.onSurface)
    }
}

