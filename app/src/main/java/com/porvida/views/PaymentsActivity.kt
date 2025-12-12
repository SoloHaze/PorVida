package com.porvida.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.porvida.AppDatabase
import com.porvida.models.Payment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Pantalla de Historial de Pagos (Compose)
 *
 * Explicación de la lógica de pagos implementada en el proyecto:
 * - Cada pago exitoso se registra como una entidad `Payment` en Room con estado "COMPLETED".
 * - En CheckoutActivity, al finalizar el pago, además de guardar el Payment, extendemos la vigencia
 *   del plan del usuario (`User.planValidUntil`) por 30 días desde la fecha mayor entre "ahora" y
 *   la vigencia actual. Con esto evitamos perder días si el usuario renueva antes de vencer.
 * - En el Dashboard se muestra el botón "Pagar plan" si: (a) no hay pagos completados, (b) el plan
 *   está vencido (now > planValidUntil o sin establecer) o (c) estamos en la ventana del 1 al 10 del
 *   mes siguiente a la fecha de expiración (ventana de renovación temprana).
 */
class PaymentsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("userId") ?: return finish()
        val db = AppDatabase.getDatabase(this)

        // StateFlow de pagos ordenados por fecha (desc)
        val paymentsFlow: StateFlow<List<Payment>> = db.paymentDao()
            .getPaymentsByUserId(userId)
            .map { it.sortedByDescending { p -> p.paymentDate } }
            .stateIn(lifecycleScope, SharingStarted.WhileSubscribed(5000), emptyList())

        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(
                background = Color(0xFF121212),
                surface = Color(0xFF121212),
                onBackground = Color(0xFFEDEDED),
                onSurface = Color(0xFFEDEDED),
                primary = Color(0xFFE53935),
                onPrimary = Color.White
            )) {
                PaymentsScreen(
                    payments = paymentsFlow,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentsScreen(
    payments: StateFlow<List<Payment>>,
    onBack: () -> Unit
) {
    val items by payments.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Historial de Pagos") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            if (items.isEmpty()) {
                Text("No tienes pagos registrados aún.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items) { p -> PaymentRow(p) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Cerrar") }
            }
        }
    }
}

@Composable
private fun PaymentRow(p: Payment) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${p.description}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("$${formatClp(p.amount)}")
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = "${formatDate(p.paymentDate)}  •  ${p.paymentMethod}  •  ${p.status}",
                color = Color.Gray
            )
        }
        Spacer(Modifier.height(8.dp))
        Divider(color = Color.DarkGray)
        Spacer(Modifier.height(8.dp))
    }
}

private fun formatClp(amount: Double): String {
    val s = amount.toInt().toString().reversed().chunked(3).joinToString(".").reversed()
    return s
}

private fun formatDate(millis: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    val y = cal.get(java.util.Calendar.YEAR)
    val m = cal.get(java.util.Calendar.MONTH) + 1
    val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
    val hh = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val mm = cal.get(java.util.Calendar.MINUTE)
    return String.format("%02d-%02d-%04d %02d:%02d", d, m, y, hh, mm)
}
