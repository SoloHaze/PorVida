package com.porvida.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.porvida.AppDatabase
import com.porvida.data.repos.CompanionRepository
import com.porvida.models.Companion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompanionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra("userId") ?: ""
        val userName = intent.getStringExtra("userName") ?: ""
    val db = AppDatabase.getDatabase(this)
    val repo = CompanionRepository(db.companionDao(), db.userDao())
        val factory = CompanionsViewModel.Factory(repo, userId)
        val vm: CompanionsViewModel by viewModels { factory }

        setContent {
            CompanionsScreen(
                userName = userName,
                vm = vm,
                onClose = { finish() }
            )
        }
    }
}

class CompanionsViewModel(
    private val repository: CompanionRepository,
    private val userId: String
) : ViewModel() {
    data class UiState(
        val active: List<Companion> = emptyList(),
        val inactive: List<Companion> = emptyList(),
        val activeCount: Int = 0
    )

    val uiState: StateFlow<UiState> = kotlinx.coroutines.flow.combine(
        repository.getActiveCompanionsByUserId(userId),
        repository.getInactiveCompanionsByUserId(userId)
    ) { a, i -> UiState(active = a, inactive = i, activeCount = a.size) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun add(
        name: String,
        lastName: String,
        birthDateMillis: Long,
        rut: String,
        address: String,
        saveForPromo: Boolean
    ) {
        viewModelScope.launch {
            repository.addCompanion(
                userId = userId,
                name = name,
                lastName = lastName,
                birthDate = birthDateMillis,
                rut = rut,
                address = address,
                saveForPromo = saveForPromo
            )
        }
    }

    fun activate(id: String) = viewModelScope.launch { repository.activateCompanion(id) }
    fun deactivate(id: String) = viewModelScope.launch { repository.deactivateCompanion(id) }
    fun delete(companion: Companion) = viewModelScope.launch { repository.deleteCompanion(companion) }

    class Factory(
        private val repository: CompanionRepository,
        private val userId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CompanionsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CompanionsViewModel(repository, userId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

private enum class CompanionsScreenMode { LIST, FORM, DETAIL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanionsScreen(userName: String, vm: CompanionsViewModel, onClose: () -> Unit) {
    var mode by remember { mutableStateOf(CompanionsScreenMode.LIST) }
    var selected by remember { mutableStateOf<Companion?>(null) }
    val uiState by vm.uiState.collectAsStateWithLifecycleCompat()

    val darkRed = Color(0xFF8B0000)
    val red = Color(0xFFD32F2F)
    val green = Color(0xFF2E7D32)

    Scaffold(
        modifier = Modifier.background(Color.Black),
        topBar = {
            TopAppBar(
                title = { Text("Acompañantes de $userName") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color(0xFFD32F2F),
                    navigationIconContentColor = Color(0xFFD32F2F),
                    actionIconContentColor = Color(0xFFD32F2F)
                ),
                modifier = Modifier.background(Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .padding(16.dp)
        ) {
            when (mode) {
                CompanionsScreenMode.LIST -> {
                    Text(
                        text = "Activos (${uiState.activeCount}/3)",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(uiState.active) { c ->
                            CompanionRow(c, true, onClick = { selected = c; mode = CompanionsScreenMode.DETAIL })
                        }
                        if (uiState.inactive.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(16.dp))
                                Text("Inactivos", color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                            }
                            items(uiState.inactive) { c ->
                                CompanionRow(c, false, onClick = { selected = c; mode = CompanionsScreenMode.DETAIL })
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { mode = CompanionsScreenMode.FORM },
                            colors = ButtonDefaults.buttonColors(containerColor = green)
                        ) { Text("Agregar Acompañante") }
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = red)
                        ) { Text("Cerrar") }
                    }
                }
                CompanionsScreenMode.FORM -> {
                    AddCompanionForm(
                        onSave = { name, lastName, birthDate, rut, address, saveForPromo ->
                            vm.add(name, lastName, birthDate, rut, address, saveForPromo)
                            mode = CompanionsScreenMode.LIST
                        },
                        onCancel = { mode = CompanionsScreenMode.LIST }
                    )
                }
                CompanionsScreenMode.DETAIL -> {
                    val c = selected
                    if (c != null) {
                        CompanionDetail(
                            companion = c,
                            onActivate = { vm.activate(c.id) },
                            onDeactivate = { vm.deactivate(c.id) },
                            onDelete = { vm.delete(c) },
                            onClose = { mode = CompanionsScreenMode.LIST }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanionRow(c: Companion, isActive: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("${c.name} ${c.lastName}", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("RUT: ${c.rut}", color = Color.Gray)
            Text("Dirección: ${c.address}", color = Color.Gray)
        }
        Text(if (isActive) "Activo" else "Inactivo", color = if (isActive) Color(0xFF66BB6A) else Color(0xFFEF5350))
    }
    Divider(color = Color(0x22FFFFFF))
}

@Composable
private fun AddCompanionForm(
    onSave: (String, String, Long, String, String, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    val red = Color(0xFFD32F2F)
    val darkRed = Color(0xFF8B0000)
    var names by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") } // formato simple ddMMyyyy o milis
    var rut by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var savePromo by remember { mutableStateOf(false) }

    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        Text("Formulario Datos Acompañante", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = names,
            onValueChange = { names = it },
            label = { Text("Nombre(s)") },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color(0xFFD32F2F),
                unfocusedTextColor = Color(0xFFD32F2F),
                cursorColor = Color(0xFFD32F2F),
                focusedLabelColor = Color(0xFFD32F2F),
                unfocusedLabelColor = Color(0xFFD32F2F),
                focusedIndicatorColor = Color(0xFFD32F2F),
                unfocusedIndicatorColor = Color(0xFFD32F2F),
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedPlaceholderColor = Color(0xFFD32F2F),
                unfocusedPlaceholderColor = Color(0xFFD32F2F)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Apellidos") },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color(0xFFD32F2F),
                unfocusedTextColor = Color(0xFFD32F2F),
                cursorColor = Color(0xFFD32F2F),
                focusedLabelColor = Color(0xFFD32F2F),
                unfocusedLabelColor = Color(0xFFD32F2F),
                focusedIndicatorColor = Color(0xFFD32F2F),
                unfocusedIndicatorColor = Color(0xFFD32F2F),
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedPlaceholderColor = Color(0xFFD32F2F),
                unfocusedPlaceholderColor = Color(0xFFD32F2F)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = birthDate,
            onValueChange = { birthDate = it },
            label = { Text("Fecha de nacimiento (millis)") },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color(0xFFD32F2F),
                unfocusedTextColor = Color(0xFFD32F2F),
                cursorColor = Color(0xFFD32F2F),
                focusedLabelColor = Color(0xFFD32F2F),
                unfocusedLabelColor = Color(0xFFD32F2F),
                focusedIndicatorColor = Color(0xFFD32F2F),
                unfocusedIndicatorColor = Color(0xFFD32F2F),
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedPlaceholderColor = Color(0xFFD32F2F),
                unfocusedPlaceholderColor = Color(0xFFD32F2F)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = rut,
            onValueChange = { rut = it },
            label = { Text("Rut") },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color(0xFFD32F2F),
                unfocusedTextColor = Color(0xFFD32F2F),
                cursorColor = Color(0xFFD32F2F),
                focusedLabelColor = Color(0xFFD32F2F),
                unfocusedLabelColor = Color(0xFFD32F2F),
                focusedIndicatorColor = Color(0xFFD32F2F),
                unfocusedIndicatorColor = Color(0xFFD32F2F),
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedPlaceholderColor = Color(0xFFD32F2F),
                unfocusedPlaceholderColor = Color(0xFFD32F2F)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Dirección") },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color(0xFFD32F2F),
                unfocusedTextColor = Color(0xFFD32F2F),
                cursorColor = Color(0xFFD32F2F),
                focusedLabelColor = Color(0xFFD32F2F),
                unfocusedLabelColor = Color(0xFFD32F2F),
                focusedIndicatorColor = Color(0xFFD32F2F),
                unfocusedIndicatorColor = Color(0xFFD32F2F),
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedPlaceholderColor = Color(0xFFD32F2F),
                unfocusedPlaceholderColor = Color(0xFFD32F2F)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = savePromo, onCheckedChange = { savePromo = it })
            Text("Guardar acompañante", color = Color(0xFFD32F2F))
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val millis = birthDate.toLongOrNull() ?: 0L
                onSave(names.trim(), lastName.trim(), millis, rut.trim(), address.trim(), savePromo)
            }, colors = ButtonDefaults.buttonColors(containerColor = red)) {
                Text("Guardar")
            }
            Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = darkRed)) {
                Text("Cerrar")
            }
        }
    }
}

@Composable
private fun CompanionDetail(
    companion: Companion,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val green = Color(0xFF2E7D32)
    val red = Color(0xFFD32F2F)
    val darkRed = Color(0xFF8B0000)
    val context = androidx.compose.ui.platform.LocalContext.current
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        Text("${companion.name} ${companion.lastName}", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("RUT: ${companion.rut}", color = Color.Gray)
        Text("Dirección: ${companion.address}", color = Color.Gray)
        Text("Activo: ${companion.isActive}", color = if (companion.isActive) Color(0xFF66BB6A) else Color(0xFFEF5350))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (companion.isActive) {
                Button(onClick = {
                    // Acceder a sedes del usuario
                    context.startActivity(android.content.Intent(context, SedesActivity::class.java))
                }, colors = ButtonDefaults.buttonColors(containerColor = green)) { Text("Acceder a sede") }
                Button(onClick = onDeactivate, colors = ButtonDefaults.buttonColors(containerColor = red)) { Text("Desactivar") }
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = darkRed)) { Text("Cerrar") }
            } else {
                Button(onClick = onActivate, colors = ButtonDefaults.buttonColors(containerColor = green)) { Text("Activar") }
                Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = red)) { Text("Eliminar") }
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = darkRed)) { Text("Cerrar") }
            }
        }
    }
}

// Helper to collect StateFlow without adding a dependency to lifecycle-runtime-compose in this context
@Composable
private fun <T> StateFlow<T>.collectAsStateWithLifecycleCompat(): androidx.compose.runtime.State<T> {
    val state = remember { mutableStateOf(value) }
    LaunchedEffect(this) {
        this@collectAsStateWithLifecycleCompat.collectLatest { state.value = it }
    }
    return state
}
