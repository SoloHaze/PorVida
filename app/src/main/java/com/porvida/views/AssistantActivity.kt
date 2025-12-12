package com.porvida.views

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.porvida.AppDatabase
import com.porvida.R
import com.porvida.data.repos.TrainingRepository
import com.porvida.models.MuscleGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssistantActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("userId") ?: ""
        val db = AppDatabase.getDatabase(this)
        val repo = TrainingRepository(db.trainingDao())
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
                AssistantScreen(userId = userId, repo = repo, db = db)
            }
        }
    }
}

@Composable
private fun AssistantScreen(userId: String, repo: TrainingRepository, db: AppDatabase) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var peso by remember { mutableStateOf("") }
    var objetivo by remember { mutableStateOf("") }
    var consejo by remember { mutableStateOf("") }
    // 1RM calculator inputs
    var calcWeight by remember { mutableStateOf("") }
    var calcReps by remember { mutableStateOf("") }

    // Mensajería simple persistida en Room (AssistantMessage)
    data class Msg(val role: String, val text: String)
    val mensajes = remember { mutableStateListOf<Msg>() }

    LaunchedEffect(userId) {
        withContext(Dispatchers.IO) {
            val saved = db.assistantMessageDao().listForUser(userId)
            val restored = saved.map { Msg(it.role, it.text) }
            withContext(Dispatchers.Main) { mensajes.addAll(restored) }
        }
    }
    var input by remember { mutableStateOf("") }

    // Cargar datos básicos del usuario desde SharedPreferences para evitar cambios de esquema
    LaunchedEffect(userId) {
        val prefs = ctx.getSharedPreferences("assistant_prefs", AppCompatActivity.MODE_PRIVATE)
        peso = prefs.getString("weight_$userId", "") ?: ""
        objetivo = prefs.getString("goal_$userId", "") ?: ""
    }

    fun kb(group: MuscleGroup): String {
        return when (group) {
            MuscleGroup.HOMBRO -> "Hombro: press militar, elevaciones laterales/frontales; máquinas de hombro."
            MuscleGroup.PECTORALES -> "Pectorales: press banca/plano e inclinado, aperturas; máquinas (press pecho, pec deck, poleas)."
            MuscleGroup.BRAZOS -> "Brazos (bíceps/tríceps): curls con barra/mancuernas, fondos, press cerrado; poleas (jalón, extensión)."
            MuscleGroup.ESPALDA -> "Espalda: dominadas, remo con barra/mancuernas; máquinas (jalón al pecho, remo sentado)."
            MuscleGroup.GLUTEO -> "Glúteos: hip thrust, sentadillas profundas; máquinas (abducción, prensa)."
            MuscleGroup.CUADRICEPS -> "Cuádriceps: sentadillas, zancadas; máquinas (prensa, extensión de cuádriceps)."
            MuscleGroup.FEMORALES -> "Femorales: peso muerto rumano; máquinas (curl femoral)."
            MuscleGroup.PANTORRILLAS -> "Pantorrillas: elevaciones de talón de pie/sentado; máquinas específicas."
            MuscleGroup.ABDOMINALES -> "Abdominales: crunch, elevación de piernas; poleas para resistencia."
            MuscleGroup.CARDIO -> "Cardio: remadora, trotadora, bici, saltos de cuerda, jumping jacks, burpees."
        }
    }

    val pageScroll = androidx.compose.foundation.rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(pageScroll)
    ) {
        Text(ctx.getString(R.string.drawer_chatbot), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        // Datos del cliente
        OutlinedTextField(value = peso, onValueChange = { peso = it.filter { ch -> ch.isDigit() } }, label = { Text("Peso (kg)") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = objetivo, onValueChange = { objetivo = it }, label = { Text("Objetivo (p.ej., bajar grasa, fuerza)") })
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val prefs = ctx.getSharedPreferences("assistant_prefs", AppCompatActivity.MODE_PRIVATE)
                prefs.edit()
                    .putString("weight_$userId", peso)
                    .putString("goal_$userId", objetivo)
                    .apply()
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("Guardar info") }
            Button(onClick = {
                scope.launch {
                    val recs = withContext(Dispatchers.IO) { repo.listAll(userId) }
                    val lastUpper = recs.firstOrNull { it.muscleGroup in listOf(
                        MuscleGroup.PECTORALES, MuscleGroup.ESPALDA, MuscleGroup.HOMBRO, MuscleGroup.BRAZOS
                    ) }
                    val cardio = recs.firstOrNull { it.muscleGroup == MuscleGroup.CARDIO }
                    val w = lastUpper?.bestWeightKg
                    val time = cardio?.timeSeconds
                    consejo = buildString {
                        append("Consejo general — ")
                        objetivo.takeIf { it.isNotBlank() }?.let { append("Objetivo: $it. ") }
                        if (w != null) append("Tu último récord de fuerza: ${w} kg. Intenta progresión +2.5 kg si fue cómodo. ")
                        if (time != null) {
                            val mm = time / 60; val ss = time % 60
                            append("Cardio mejor tiempo: %02d:%02d. Intervalos 4x (2' rápido / 1' lento).".format(mm, ss))
                        }
                        if (w == null && time == null) append("Registra tus entrenamientos para recomendaciones más precisas.")
                    }
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Consejo") }
        }

        Spacer(Modifier.height(12.dp))
        if (consejo.isNotBlank()) {
            Text(consejo, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
        }

        // 1RM Estimator (Epley) and quick maxima
        Text("Calculadora 1RM (estimado)", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = calcWeight, onValueChange = { calcWeight = it.filter { ch -> ch.isDigit() || ch=='.' } }, label = { Text("Peso (kg)") })
            OutlinedTextField(value = calcReps, onValueChange = { calcReps = it.filter { ch -> ch.isDigit() } }, label = { Text("Reps") })
            Button(onClick = {}, enabled = false, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) {
                val w = calcWeight.toFloatOrNull()
                val r = calcReps.toIntOrNull()
                val oneRm = if (w!=null && r!=null && r>0) w * (1f + r/30f) else null
                Text(oneRm?.let { "1RM≈ ${"%.1f".format(it)} kg" } ?: "1RM≈ ?")
            }
        }
        Spacer(Modifier.height(8.dp))
        var maxes by remember { mutableStateOf<Map<MuscleGroup, Float>>(emptyMap()) }
        LaunchedEffect(userId) {
            withContext(Dispatchers.IO) {
                val all = repo.listAll(userId)
                val by = all.groupBy { it.muscleGroup }
                val m = by.mapValues { (_, list) -> list.mapNotNull { it.bestWeightKg }.maxOrNull() ?: 0f }
                withContext(Dispatchers.Main) { maxes = m }
            }
        }
        if (maxes.isNotEmpty()) {
            Text("Tus máximos por grupo (kg):", color = MaterialTheme.colorScheme.onSurface)
            maxes.forEach { (g, v) -> if (v>0f) Text("• ${g.name}: ${"%.1f".format(v)} kg", color = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.height(8.dp))
        }

        // Atajos por grupo
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val recs = withContext(Dispatchers.IO) { repo.listByGroup(userId, MuscleGroup.PECTORALES) }
                    val last = recs.firstOrNull()?.bestWeightKg
                    mensajes.add(Msg("assistant", "Pecho — ${kb(MuscleGroup.PECTORALES)}" + (last?.let { "\nTu récord: ${it} kg." } ?: "")))
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("Pecho") }
            Button(onClick = {
                scope.launch {
                    val recs = withContext(Dispatchers.IO) { repo.listByGroup(userId, MuscleGroup.ESPALDA) }
                    val last = recs.firstOrNull()?.bestWeightKg
                    mensajes.add(Msg("assistant", "Espalda — ${kb(MuscleGroup.ESPALDA)}" + (last?.let { "\nTu récord: ${it} kg." } ?: "")))
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("Espalda") }
            Button(onClick = {
                scope.launch {
                    val recs = withContext(Dispatchers.IO) { repo.listByGroup(userId, MuscleGroup.CARDIO) }
                    val last = recs.firstOrNull()?.timeSeconds
                    val min = last?.div(60) ?: 0
                    val sec = last?.rem(60) ?: 0
                    mensajes.add(Msg("assistant", "Cardio — ${kb(MuscleGroup.CARDIO)}" + (last?.let { "\nMejor tiempo: %02d:%02d".format(min, sec) } ?: "")))
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("Cardio") }
        }

        Spacer(Modifier.height(12.dp))
    // Mensajes
        OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Escribe al profesor") })
        Spacer(Modifier.height(6.dp))
        Button(onClick = {
            val txt = input.trim(); if (txt.isEmpty()) return@Button
            val usr = Msg("user", txt)
            mensajes.add(usr)
            scope.launch(Dispatchers.IO) {
                db.assistantMessageDao().upsert(
                    com.porvida.models.AssistantMessage(
                        id = java.util.UUID.randomUUID().toString(),
                        userId = userId,
                        role = usr.role,
                        text = usr.text
                    )
                )
            }
            input = ""
            // Respuesta básica del profesor actual
            val resp = Msg("assistant", "Profesor: gracias por tu mensaje. Revisa técnica y mantén constancia. ¡Vamos con todo!")
            mensajes.add(resp)
            scope.launch(Dispatchers.IO) {
                db.assistantMessageDao().upsert(
                    com.porvida.models.AssistantMessage(
                        id = java.util.UUID.randomUUID().toString(),
                        userId = userId,
                        role = resp.role,
                        text = resp.text
                    )
                )
            }
        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Enviar") }

        Spacer(Modifier.height(12.dp))
        // Quick Note creator under the message box
        var noteTitle by remember { mutableStateOf("") }
        var noteDate by remember { mutableStateOf(System.currentTimeMillis()) }
        var noteReminders by remember { mutableStateOf(5) }
        Text("Programar nota del entrenamiento", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = noteTitle, onValueChange = { noteTitle = it }, label = { Text("Título de la nota (p.ej., Pecho + Hombro)") })
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { noteDate -= 24L*60*60*1000 }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("-1d") }
            Button(onClick = { noteDate += 24L*60*60*1000 }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("+1d") }
            Text(java.text.SimpleDateFormat("dd-MM-yyyy").format(java.util.Date(noteDate)), color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recordatorios: $noteReminders", color = MaterialTheme.colorScheme.primary)
            Button(onClick = { if (noteReminders > 1) noteReminders-- }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("-") }
            Button(onClick = { if (noteReminders < 10) noteReminders++ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("+") }
        }
        Spacer(Modifier.height(6.dp))
        Button(onClick = {
            val t = noteTitle.trim(); if (t.isEmpty()) return@Button
            val note = com.porvida.models.Note(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                title = t,
                details = "Plan: entrenar 3 veces a la semana cada grupo. Ajusta volumen si hubo dolor o lesión reciente.",
                dateMillis = startOfDay(noteDate),
                injuryNotes = "",
                reminders = noteReminders
            )
            scope.launch(Dispatchers.IO) {
                db.noteDao().upsert(note)
                // Schedule reminders for the day
                com.porvida.notifications.ReminderScheduler.scheduleDailyReminders(
                    ctx,
                    note.dateMillis,
                    note.title,
                    "Recuerda tu rutina del día",
                    note.reminders,
                    userId,
                    note.id,
                    firstMinutesOfDay = null
                )
            }
            noteTitle = ""
        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Crear nota y recordatorios") }

        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(mensajes.size) {
            if (mensajes.isNotEmpty()) {
                listState.animateScrollToItem(mensajes.size - 1)
            }
        }
    Box(Modifier.weight(1f)) {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.matchParentSize()) {
                items(mensajes) { m ->
                val color = if (m.role == "user") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                Text((if (m.role == "user") "Tú: " else "Asistente: ") + m.text, color = color)
                }
            }
            // Scroll to bottom button
            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    if (mensajes.isNotEmpty()) scope.launch { listState.animateScrollToItem(mensajes.size - 1) }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(8.dp)
            ) { Text("▼") }
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
