package com.porvida.views

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import android.content.Context
import com.porvida.AppDatabase
import com.porvida.models.Note
import com.porvida.models.MuscleGroup
import com.porvida.data.repos.ClassRepository
import com.porvida.models.ClassSession
import com.porvida.models.ClassType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("userId") ?: ""
        val db = AppDatabase.getDatabase(this)
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
                NotesScreen(userId = userId, db = db)
            }
        }
    }
}

@Composable
private fun NotesScreen(userId: String, db: AppDatabase) {
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var injury by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }
    var reminders by remember { mutableStateOf(5) }
    // Optional time-of-day for the note (minutes from 0..1439)
    var timeOfDayMinutes by remember { mutableStateOf<Int?>(null) }
    val notes = remember { mutableStateListOf<Note>() }
    val classRepo = remember(db) { ClassRepository(db.classDao()) }
    // detected or default sede; could be improved by reading user's last selected sede
    val sedeId = remember { "sede_concha_toro" }
    var classesByDay by remember { mutableStateOf<Map<Long, List<ClassSession>>>(emptyMap()) }
    var classesForSelected by remember { mutableStateOf<List<ClassSession>>(emptyList()) }

    LaunchedEffect(userId) {
        val loaded = withContext(Dispatchers.IO) { db.noteDao().listForUser(userId) }
        notes.clear(); notes.addAll(loaded)
    }

    // Load classes for the month around selectedDate
    LaunchedEffect(selectedDate, sedeId) {
        val monthCal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate; set(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
        val from = monthCal.timeInMillis
        monthCal.add(java.util.Calendar.MONTH, 1)
        val to = monthCal.timeInMillis - 1
        val sessions = withContext(Dispatchers.IO) { classRepo.listSessionsInRange(sedeId, from, to) }
        val byDay = sessions.groupBy { startOfDay(it.dateTimeMillis) }
        classesByDay = byDay
        classesForSelected = byDay[startOfDay(selectedDate)] ?: emptyList()
    }

    fun pickDateDialog(ctx: Context, current: Long, onPick: (Long) -> Unit) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = current }
        val dlg = android.app.DatePickerDialog(
            ctx,
            { _, y, m, d ->
                val c = java.util.Calendar.getInstance()
                c.set(y, m, d, 0, 0, 0)
                c.set(java.util.Calendar.MILLISECOND, 0)
                onPick(c.timeInMillis)
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
        dlg.show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Notas y planificación", color = MaterialTheme.colorScheme.primary) }
        item {
            MonthCalendar(
                selectedDate = selectedDate,
                onPreviousMonth = {
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }
                    cal.add(java.util.Calendar.MONTH, -1)
                    selectedDate = cal.timeInMillis
                },
                onNextMonth = {
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }
                    cal.add(java.util.Calendar.MONTH, 1)
                    selectedDate = cal.timeInMillis
                },
                onSelectDay = { dayMillis ->
                    selectedDate = dayMillis
                    classesForSelected = classesByDay[startOfDay(dayMillis)] ?: emptyList()
                },
                classesByDay = classesByDay
            )
        }
        // Daily class list for the selected day
        item {
            if (classesForSelected.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(10.dp)) {
                    Text("Clases del día", color = MaterialTheme.colorScheme.primary)
                    classesForSelected.sortedBy { it.dateTimeMillis }.forEach { s ->
                        val hour = java.text.SimpleDateFormat("HH:mm").format(java.util.Date(s.dateTimeMillis))
                        val label = when (s.type) {
                            ClassType.CROSSFIT -> "Crossfit"
                            ClassType.WORKBODY -> "Workbody"
                            ClassType.RITMO -> "Ritmo"
                            ClassType.MACHINEFIT -> "MachineFit"
                        }
                        Text("• $hour  $label", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Detalles / rutina del día") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = injury, onValueChange = { injury = it }, label = { Text("Antecedentes de lesiones / cuidados") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { expanded = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) {
                        Text(if (group.isBlank()) "Grupo muscular" else group)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        MuscleGroup.values().forEach { mg ->
                            DropdownMenuItem(text = { Text(mg.name) }, onClick = { group = mg.name; expanded = false })
                        }
                    }
                }
                Button(onClick = { pickDateDialog(ctx, selectedDate) { selectedDate = it } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("Elegir fecha") }
                Text(
                    java.text.SimpleDateFormat("dd-MM-yyyy").format(java.util.Date(selectedDate)),
                    color = MaterialTheme.colorScheme.primary
                )
                // Time picker optional
                var showTime by remember { mutableStateOf(false) }
                Button(onClick = { showTime = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("Hora") }
                val selectedTimeText = timeOfDayMinutes?.let { "%02d:%02d".format(it/60, it%60) }
                if (selectedTimeText != null) {
                    Text(selectedTimeText, color = MaterialTheme.colorScheme.primary)
                }
                if (showTime) {
                    val calNow = java.util.Calendar.getInstance()
                    val th = timeOfDayMinutes?.div(60) ?: calNow.get(java.util.Calendar.HOUR_OF_DAY)
                    val tm = timeOfDayMinutes?.rem(60) ?: calNow.get(java.util.Calendar.MINUTE)
                    android.app.TimePickerDialog(ctx, { _, h, m ->
                        timeOfDayMinutes = h*60 + m
                        showTime = false
                    }, th, tm, true).apply { setOnCancelListener { showTime = false } }.show()
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Recordatorios: $reminders", color = MaterialTheme.colorScheme.primary)
                Button(onClick = { if (reminders > 1) reminders-- }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("-") }
                Button(onClick = { if (reminders < 10) reminders++ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("+") }
            }
        }
        item {
            Button(onClick = {
                if (title.isBlank()) return@Button
                val note = Note(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    title = title,
                    details = details,
                    dateMillis = startOfDay(selectedDate),
                    timeOfDayMinutes = timeOfDayMinutes,
                    muscleGroup = group,
                    injuryNotes = injury,
                    reminders = reminders
                )
                scope.launch(Dispatchers.IO) {
                    db.noteDao().upsert(note)
                    com.porvida.notifications.ReminderScheduler.scheduleDailyReminders(
                        ctx,
                        note.dateMillis,
                        note.title,
                        "Recuerda tu rutina del día",
                        reminders,
                        userId,
                        note.id,
                        firstMinutesOfDay = timeOfDayMinutes
                    )
                    withContext(Dispatchers.Main) {
                        notes.add(note)
                        title = ""; details = ""; injury = ""; group = ""; reminders = 5; timeOfDayMinutes = null
                    }
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Guardar nota") }
        }
        item { Spacer(Modifier.height(4.dp)) }
        items(notes, key = { it.id }) { n ->
            NoteRow(n,
                onToggleComplete = { toggled ->
                    scope.launch(Dispatchers.IO) { db.noteDao().upsert(n.copy(completed = toggled)) }
                },
                onEdit = {
                    scope.launch(Dispatchers.IO) {
                        db.noteDao().upsert(n.copy(details = n.details + "\n(Editar: ajusta volumen si hubo dolor)") )
                        val refreshed = db.noteDao().listForUser(userId)
                        withContext(Dispatchers.Main) { notes.clear(); notes.addAll(refreshed) }
                    }
                },
                onDelete = {
                    scope.launch(Dispatchers.IO) {
                        db.noteDao().delete(n.id)
                        val refreshed = db.noteDao().listForUser(userId)
                        withContext(Dispatchers.Main) { notes.clear(); notes.addAll(refreshed) }
                    }
                }
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MonthCalendar(
    selectedDate: Long,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Long) -> Unit,
    classesByDay: Map<Long, List<ClassSession>> = emptyMap()
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val cal = remember(selectedDate) {
        java.util.Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
    }
    val monthYear = remember(cal.timeInMillis) {
        java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(cal.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }
    val firstDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) // 1=Sunday
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val startOffset = (if (firstDayOfWeek == java.util.Calendar.SUNDAY) 0 else firstDayOfWeek - 1) // make Monday=1 style grid with Sunday at 0

    Column(Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = "Mes anterior", tint = primary, modifier = Modifier.clickable { onPreviousMonth() })
            Text(monthYear, color = primary)
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = "Mes siguiente", tint = primary, modifier = Modifier.clickable { onNextMonth() })
        }
        Spacer(Modifier.height(8.dp))
        // Weekday headers (Mon..Sun)
        val weekdays = listOf("L", "M", "X", "J", "V", "S", "D")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            weekdays.forEach { d -> Text(d, color = onSurface.copy(alpha = 0.7f), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        Spacer(Modifier.height(6.dp))
        // Grid 6 rows x 7 cols
        var dayNum = 1
        repeat(6) { _ ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(7) { cellIdx ->
                    val idx = cellIdx + (dayNum - 1) + startOffset
                    val showNumber = idx in startOffset until (startOffset + daysInMonth)
                    if (showNumber) {
                        val day = dayNum
                        val isSelected = java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }.get(java.util.Calendar.DAY_OF_MONTH) == day &&
                                java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }.get(java.util.Calendar.MONTH) == cal.get(java.util.Calendar.MONTH) &&
                                java.util.Calendar.getInstance().apply { timeInMillis = selectedDate }.get(java.util.Calendar.YEAR) == cal.get(java.util.Calendar.YEAR)
                        val bg = if (isSelected) primary.copy(alpha = 0.25f) else Color.Transparent
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .background(bg)
                                .clickable {
                                    val c = java.util.Calendar.getInstance().apply {
                                        timeInMillis = cal.timeInMillis
                                        set(java.util.Calendar.DAY_OF_MONTH, day)
                                    }
                                    onSelectDay(c.timeInMillis)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day.toString(), color = onSurface)
                                val thisDayMillis = java.util.Calendar.getInstance().apply {
                                    timeInMillis = cal.timeInMillis
                                    set(java.util.Calendar.DAY_OF_MONTH, day)
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                if (!classesByDay[thisDayMillis].isNullOrEmpty()) {
                                    Spacer(Modifier.height(2.dp))
                                    Box(Modifier.size(6.dp).background(primary, shape = androidx.compose.foundation.shape.CircleShape))
                                }
                            }
                        }
                        dayNum++
                    } else {
                        Box(modifier = Modifier.weight(1f).padding(2.dp)) { }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(n: Note, onToggleComplete: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(n.title, color = MaterialTheme.colorScheme.onSurface)
            if (n.details.isNotBlank()) Text(n.details, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            val dateStr = java.text.SimpleDateFormat("dd-MM-yyyy").format(java.util.Date(n.dateMillis))
            val timeStr = n.timeOfDayMinutes?.let { " %02d:%02d".format(it/60, it%60) } ?: ""
            Text("Fecha: $dateStr$timeStr  ${if (n.muscleGroup.isNotBlank()) "• ${n.muscleGroup}" else ""}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            if (n.injuryNotes.isNotBlank()) Text("Cuidado: ${n.injuryNotes}", color = Color(0xFFFFB74D))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val label = if (n.completed) "Hecha" else "Pendiente"
            Button(onClick = { onToggleComplete(!n.completed) }, colors = ButtonDefaults.buttonColors(containerColor = if (n.completed) Color(0xFF2E7D32) else Color(0xFFC62828))) { Text(label) }
            Button(onClick = onEdit, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("Editar") }
            Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))) { Text("Eliminar") }
        }
    }
}

private fun startOfDay(millis: Long): Long {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
