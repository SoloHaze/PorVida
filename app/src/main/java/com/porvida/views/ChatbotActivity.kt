package com.porvida.views

import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import com.porvida.R
import com.porvida.AppDatabase
import com.porvida.data.repos.TrainingRepository
import com.porvida.models.ChatMessage
import com.porvida.models.MuscleGroup
import com.porvida.network.OpenAIService
import com.porvida.network.OpenAIService.MessageItem
import com.porvida.network.OpenAIBroadcastReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatbotActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("userId") ?: ""
    val db = AppDatabase.getDatabase(this)
    val conversationId = intent.getStringExtra("conversationId") ?: "default"
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
                ChatbotScreen(userId, repo, db, conversationId)
            }
        }
    }
}

@Composable
private fun ChatbotScreen(userId: String, repo: TrainingRepository, db: AppDatabase, conversationId: String) {
    val scope = rememberCoroutineScope()
    var answer by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var enableWeb by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<MessageItem>() }
    var allowedDomains by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("") }
    var citations by remember { mutableStateOf(listOf<String>()) }
    var isSending by remember { mutableStateOf(false) }

    // Load saved chat history for this user
    LaunchedEffect(userId, conversationId) {
        withContext(Dispatchers.IO) {
            val saved = db.chatDao().listForUser(userId, conversationId)
            val restored = saved.map { MessageItem(role = it.role, content = it.content) }
            withContext(Dispatchers.Main) {
                history.clear()
                history.addAll(restored)
            }
        }
    }

    // Register a BroadcastReceiver for responses
    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == OpenAIBroadcastReceiver.ACTION_RESPONSE) {
                    val error = intent.getStringExtra(OpenAIBroadcastReceiver.EXTRA_ERROR)
                    val text = intent.getStringExtra(OpenAIBroadcastReceiver.EXTRA_TEXT)
                    val cites = intent.getStringArrayListExtra(OpenAIBroadcastReceiver.EXTRA_CITATIONS)
                    if (!error.isNullOrBlank()) {
                        answer = "Error: $error"
                        citations = emptyList()
                        isSending = false
                    } else if (!text.isNullOrBlank()) {
                        answer = text
                        val assistantMsg = MessageItem(role = "assistant", content = text)
                        history.add(assistantMsg)
                        // persist assistant message
                        scope.launch(Dispatchers.IO) {
                            db.chatDao().upsert(
                                ChatMessage(
                                    id = java.util.UUID.randomUUID().toString(),
                                    userId = userId,
                                    conversationId = conversationId,
                                    role = assistantMsg.role,
                                    content = assistantMsg.content
                                )
                            )
                        }
                        citations = cites?.toList() ?: emptyList()
                        isSending = false
                    }
                } else if (intent?.action == com.porvida.network.GeminiBroadcastReceiver.ACTION_RESPONSE) {
                    val error = intent.getStringExtra(com.porvida.network.GeminiBroadcastReceiver.EXTRA_ERROR)
                    val text = intent.getStringExtra(com.porvida.network.GeminiBroadcastReceiver.EXTRA_TEXT)
                    if (!error.isNullOrBlank()) {
                        answer = "Error: $error"
                        citations = emptyList()
                        isSending = false
                    } else if (!text.isNullOrBlank()) {
                        answer = text
                        val assistantMsg = MessageItem(role = "assistant", content = text)
                        history.add(assistantMsg)
                        // persist assistant message
                        scope.launch(Dispatchers.IO) {
                            db.chatDao().upsert(
                                ChatMessage(
                                    id = java.util.UUID.randomUUID().toString(),
                                    userId = userId,
                                    conversationId = conversationId,
                                    role = assistantMsg.role,
                                    content = assistantMsg.content
                                )
                            )
                        }
                        citations = emptyList()
                        isSending = false
                    }
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(OpenAIBroadcastReceiver.ACTION_RESPONSE)
            addAction(com.porvida.network.GeminiBroadcastReceiver.ACTION_RESPONSE)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                ctx.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) { }
        onDispose {
            try { ctx.unregisterReceiver(receiver) } catch (_: Exception) { }
        }
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
        Text(LocalContext.current.getString(R.string.chatbot_title), color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)), onClick = {
                scope.launch {
                    val recs = withContext(Dispatchers.IO) { repo.listByGroup(userId, MuscleGroup.PECTORALES) }
                    val last = recs.firstOrNull()?.bestWeightKg
                    answer = "Pecho — ${kb(MuscleGroup.PECTORALES)}" + (last?.let { "\nTu récord de press banca: ${it} kg. Prueba +5 kg si fue cómodo." } ?: "")
                }
            }) { Text(LocalContext.current.getString(R.string.chatbot_pecho), color = MaterialTheme.colorScheme.primary) }

            Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)), onClick = {
                scope.launch {
                    val recs = withContext(Dispatchers.IO) { repo.listByGroup(userId, MuscleGroup.ESPALDA) }
                    val last = recs.firstOrNull()?.bestWeightKg
                    answer = "Espalda — ${kb(MuscleGroup.ESPALDA)}" + (last?.let { "\nTu récord de remo: ${it} kg." } ?: "")
                }
            }) { Text(LocalContext.current.getString(R.string.chatbot_espalda), color = MaterialTheme.colorScheme.primary) }

            Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)), onClick = {
                scope.launch {
                    val recs = withContext(Dispatchers.IO) { repo.listByGroup(userId, MuscleGroup.CARDIO) }
                    val last = recs.firstOrNull()?.timeSeconds
                    val min = last?.div(60) ?: 0
                    val sec = last?.rem(60) ?: 0
                    answer = "Cardio — ${kb(MuscleGroup.CARDIO)}" + (last?.let { "\nTu mejor tiempo: %02d:%02d".format(min, sec) } ?: "")
                }
            }) { Text("Cardio", color = MaterialTheme.colorScheme.primary) }
        }

        Spacer(Modifier.height(16.dp))
        // Simple ask box to AI (Gemini/OpenAI)
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Pregunta al asistente (IA)") }
        )
        Spacer(Modifier.height(8.dp))
        if (enableWeb) {
            androidx.compose.material3.OutlinedTextField(
                value = allowedDomains,
                onValueChange = { allowedDomains = it },
                label = { Text("Allowed domains (comma-separated)") }
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country (ISO-2)") })
                androidx.compose.material3.OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") })
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Region") })
                androidx.compose.material3.OutlinedTextField(value = timezone, onValueChange = { timezone = it }, label = { Text("Timezone (IANA)") })
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.FilterChip(
                selected = enableWeb,
                onClick = { enableWeb = !enableWeb },
                label = { Text("Web search") }
            )
            Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), onClick = {
                if (query.isBlank()) return@Button
                val userMsg = MessageItem(role = "user", content = query)
                history.add(userMsg)
                // persist user message
                scope.launch(Dispatchers.IO) {
                    db.chatDao().upsert(
                        ChatMessage(
                            id = java.util.UUID.randomUUID().toString(),
                            userId = userId,
                            conversationId = conversationId,
                            role = userMsg.role,
                            content = userMsg.content
                        )
                    )
                }
                // Serialize history to JSON for the Java receiver
                citations = emptyList()
                isSending = true
                // Prefer Gemini if configured; fallback to OpenAI otherwise
                if (com.porvida.BuildConfig.GEMINI_API_KEY.isNotBlank()) {
                    val gIntent = android.content.Intent(com.porvida.network.GeminiBroadcastReceiver.ACTION_REQUEST).apply {
                        putExtra(com.porvida.network.GeminiBroadcastReceiver.EXTRA_PROMPT, userMsg.content)
                    }
                    ctx.sendBroadcast(gIntent)
                } else {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val ser = kotlinx.serialization.builtins.ListSerializer(OpenAIService.MessageItem.serializer())
                    val historyJson = json.encodeToString(ser, history.toList())
                    val intent = android.content.Intent(OpenAIBroadcastReceiver.ACTION_REQUEST).apply {
                        putExtra(OpenAIBroadcastReceiver.EXTRA_HISTORY_JSON, historyJson)
                        putExtra(OpenAIBroadcastReceiver.EXTRA_ENABLE_WEB, enableWeb)
                        putExtra(OpenAIBroadcastReceiver.EXTRA_MODEL, "gpt-5-nano")
                        if (allowedDomains.isNotBlank()) putExtra(OpenAIBroadcastReceiver.EXTRA_ALLOWED_DOMAINS, allowedDomains.split(',').map { it.trim() }.toTypedArray())
                        if (country.isNotBlank()) putExtra(OpenAIBroadcastReceiver.EXTRA_COUNTRY, country)
                        if (city.isNotBlank()) putExtra(OpenAIBroadcastReceiver.EXTRA_CITY, city)
                        if (region.isNotBlank()) putExtra(OpenAIBroadcastReceiver.EXTRA_REGION, region)
                        if (timezone.isNotBlank()) putExtra(OpenAIBroadcastReceiver.EXTRA_TIMEZONE, timezone)
                    }
                    ctx.sendBroadcast(intent)
                }
                query = ""
                // Safety timeout: if no response in 30s, reset sending state
                scope.launch {
                    kotlinx.coroutines.delay(30_000)
                    if (isSending) {
                        answer = "Error: tiempo de espera agotado (sin respuesta del servicio)"
                        isSending = false
                    }
                }
            }) { Text(if (isSending) "Enviando..." else "Enviar", color = Color.White) }
            Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)), onClick = {
                history.clear(); answer = ""; query = ""
                // clear persisted history
                scope.launch(Dispatchers.IO) { db.chatDao().clearForUser(userId, conversationId) }
            }) { Text("Limpiar", color = Color.White) }
        }

        Spacer(Modifier.height(12.dp))
        // Chat transcript
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(history.size) {
            if (history.isNotEmpty()) {
                listState.animateScrollToItem(history.size - 1)
            }
        }
        Box(Modifier.weight(1f)) {
            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                modifier = Modifier.matchParentSize(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
            ) {
                items(history.size) { idx ->
                val m = history[idx]
                val isUser = m.role == "user"
                val bubbleColor = if (isUser) Color(0xFF263238) else Color(0xFF1E1E1E)
                val textColor = MaterialTheme.colorScheme.onSurface
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) androidx.compose.foundation.layout.Arrangement.End else androidx.compose.foundation.layout.Arrangement.Start
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .background(bubbleColor)
                            .padding(10.dp)
                            .widthIn(max = 320.dp)
                    ) {
                        Text((if (isUser) "Tú: " else "Asistente: ") + m.content, color = textColor)
                    }
                }
                }
            }
            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    if (history.isNotEmpty()) scope.launch { listState.animateScrollToItem(history.size - 1) }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(8.dp)
            ) { Text("▼") }
        }
        if (answer.startsWith("Error:")) {
            Spacer(Modifier.height(8.dp))
            Text(answer, color = Color(0xFFE57373))
        }
        if (citations.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Citas:", color = MaterialTheme.colorScheme.primary)
            for (u in citations) {
                androidx.compose.material3.TextButton(onClick = {
                    val i = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(u))
                    ctx.startActivity(i)
                }) { Text(u) }
            }
        }
    }
}

