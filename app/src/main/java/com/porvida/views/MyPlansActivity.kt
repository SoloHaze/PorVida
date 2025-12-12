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
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.porvida.AppDatabase
import com.porvida.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyPlansActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("userId") ?: ""
        val userName = intent.getStringExtra("userName") ?: "Cliente"
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
                PlansScreen(userId = userId, userName = userName, db = db)
            }
        }
    }
}

@Composable
private fun PlansScreen(userId: String, userName: String, db: AppDatabase) {
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<User?>(null) }
    var companions by remember { mutableStateOf(listOf<com.porvida.models.Companion>()) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

        LaunchedEffect(userId) {
            val u = withContext(Dispatchers.IO) { db.userDao().getUserById(userId) }
            user = u
            // Collect flow on main; Room emits on background thread internally
            db.companionDao().getActiveCompanionsByUserId(userId).collect { list -> companions = list }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Text("Planes contratados", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        val u = user
        if (u != null) {
            val valid = if (u.planValidUntil > 0) java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(u.planValidUntil)) else "—"
            Text("Mi plan: ${u.plan}", color = MaterialTheme.colorScheme.onSurface)
            Text("Válido hasta: $valid", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    // Cambiar/Adquirir plan para mí
                    val i = android.content.Intent(ctx, PlanesActivity::class.java).apply {
                        putExtra("userId", userId)
                        putExtra("currentPlan", u.plan)
                    }
                    ctx.startActivity(i)
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Cambiar/Adquirir para mí") }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Acompañantes", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(companions, key = { it.id }) { c ->
                Column(Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(10.dp)) {
                    Text("${c.name} ${c.lastName}", color = MaterialTheme.colorScheme.onSurface)
                    val email = c.email ?: "(sin email)"
                    Text("Email: $email", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            // Abrir selector de plan para el acompañante
                            val i = android.content.Intent(ctx, PlanesActivity::class.java).apply {
                                putExtra("userId", userId) // quien paga
                                putExtra("companionId", c.id)
                                putExtra("companionEmail", c.email)
                                putExtra("companionName", "${c.name} ${c.lastName}")
                            }
                            ctx.startActivity(i)
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))) { Text("Pagar plan para acompañante", color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        }
    }
}
