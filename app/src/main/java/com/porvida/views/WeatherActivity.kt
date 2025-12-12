package com.porvida.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.porvida.viewmodel.WeatherViewModel
import com.porvida.models.*

class WeatherActivity: ComponentActivity() {
    private val vm: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                WeatherScreen(vm = vm, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherScreen(vm: WeatherViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("auto:ip") }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Clima") }, actions = {
            TextButton(onClick = onBack) { Text("Cerrar", color = MaterialTheme.colorScheme.primary) }
        })
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                is WeatherUiState.Loading -> LoadingView()
                is WeatherUiState.Error -> ErrorView((state as WeatherUiState.Error).message) { vm.refresh(query) }
                is WeatherUiState.Success -> SuccessView((state as WeatherUiState.Success).data, query, onQueryChange = { query = it }, onRefresh = { vm.refresh(query) })
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorView(msg: String, retry: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Error: $msg", color = Color.Red)
        Button(onClick = retry) { Text("Reintentar") }
    }
}

@Composable
private fun SuccessView(data: WeatherResponse, query: String, onQueryChange: (String) -> Unit, onRefresh: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = query, onValueChange = onQueryChange, label = { Text("Ubicación (ej: auto:ip / Santiago / 48.85,2.35)") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Button(onClick = onRefresh) { Text("Actualizar") }
            }
        }
        item { LocationBlock(data.location) }
        item { CurrentBlock(data.current) }
        item { AirQualityBlock(data.current.airQuality) }
        item { PollenBlock(data.current.pollen) }
        if (data.alerts?.alert?.isNotEmpty() == true) {
            item { AlertsBlock(data.alerts.alert!!) }
        }
        item { ForecastBlock(data.forecast) }
    }
}

@Composable
private fun LocationBlock(loc: Location) {
    Card { Column(Modifier.padding(12.dp)) {
        Text("Ubicación", fontWeight = FontWeight.Bold)
        Text("${loc.name}, ${loc.region}, ${loc.country}")
        Text("Zona horaria: ${loc.tz_id}")
        Text("Hora local: ${loc.localtime}")
        Text("Lat/Lon: ${loc.lat}, ${loc.lon}")
    }}
}

@Composable
private fun CurrentBlock(cur: Current) {
    Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Clima Actual", fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = iconUrl(cur.condition?.icon), contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.width(12.dp))
            Column { Text("${cur.temp_c?.let { "${it}°C" } ?: "--"} | Sensación: ${cur.feelslike_c?.let { "${it}°C" } ?: "--"}")
                Text(cur.condition?.text ?: "--") }
        }
        Text("Viento: ${cur.wind_kph ?: 0f} km/h ${cur.wind_dir ?: ""}")
        Text("Humedad: ${cur.humidity ?: 0}% | UV: ${cur.uv ?: 0f}")
        Text("Presión: ${cur.pressure_mb ?: 0f} mb | Precipitación: ${cur.precip_mm ?: 0f} mm")
        Text("Nubes: ${cur.cloud ?: 0}% | Día? ${if (cur.is_day == 1) "Sí" else "No"}")
    }}
}

private fun iconUrl(partial: String?): String? = partial?.let { if (it.startsWith("//")) "https:$it" else it }

@Composable
private fun AirQualityBlock(aq: AirQuality?) {
    Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Calidad del Aire", fontWeight = FontWeight.Bold)
        if (aq == null) { Text("No disponible") } else {
            Text("CO: ${aq.co ?: 0f} | O3: ${aq.o3 ?: 0f}")
            Text("NO2: ${aq.no2 ?: 0f} | SO2: ${aq.so2 ?: 0f}")
            Text("PM2.5: ${aq.pm2_5 ?: 0f} | PM10: ${aq.pm10 ?: 0f}")
            Text("US EPA: ${aq.usEpaIndex ?: 0} (${epaLabel(aq.usEpaIndex)})")
        }
    }}
}

private fun epaLabel(idx: Int?): String = when(idx) {
    1 -> "Bueno"
    2 -> "Moderado"
    3 -> "Sensibles: Precaución"
    4 -> "No saludable"
    5 -> "Muy no saludable"
    6 -> "Peligroso"
    else -> "--"
}

@Composable
private fun PollenBlock(p: Pollen?) {
    Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Polen", fontWeight = FontWeight.Bold)
        if (p == null) Text("No disponible") else {
            Text("Grass: ${level(p.Grass)} | Oak: ${level(p.Oak)} | Birch: ${level(p.Birch)}")
            Text("Hazel: ${level(p.Hazel)} | Alder: ${level(p.Alder)}")
            Text("Mugwort: ${level(p.Mugwort)} | Ragweed: ${level(p.Ragweed)}")
        }
    }}
}

private fun level(v: Float?): String {
    val x = v ?: return "--"
    return when {
        x < 20 -> "Bajo (${x})"
        x < 100 -> "Moderado (${x})"
        x < 300 -> "Alto (${x})"
        else -> "Muy Alto (${x})"
    }
}

@Composable
private fun AlertsBlock(alerts: List<Alert>) {
    Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Alertas", fontWeight = FontWeight.Bold)
        alerts.forEach { a ->
            Column { Text(a.headline ?: a.event ?: "Alerta", fontWeight = FontWeight.SemiBold)
                Text(a.severity ?: "")
                if (!a.desc.isNullOrBlank()) Text(a.desc!!.take(180) + if (a.desc!!.length > 180) "…" else "")
                if (!a.instruction.isNullOrBlank()) Text("Instrucción: ${a.instruction!!.take(140)}" )
            }
        }
    }}
}

@Composable
private fun ForecastBlock(forecast: Forecast?) {
    if (forecast == null) return
    Card { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pronóstico Próximos Días", fontWeight = FontWeight.Bold)
        forecast.forecastday.take(3).forEach { fd ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AsyncImage(model = iconUrl(fd.day.condition?.icon), contentDescription = null, modifier = Modifier.size(48.dp))
                Column(Modifier.padding(start = 8.dp).weight(1f)) {
                    Text(fd.date)
                    Text("Máx ${fd.day.maxtemp_c ?: 0f}°C / Mín ${fd.day.mintemp_c ?: 0f}°C")
                    Text("Lluvia: ${fd.day.daily_chance_of_rain ?: 0}% Viento: ${fd.day.maxwind_kph ?: 0f} km/h")
                }
                Text(fd.day.condition?.text ?: "")
            }
        }
    }}
}
