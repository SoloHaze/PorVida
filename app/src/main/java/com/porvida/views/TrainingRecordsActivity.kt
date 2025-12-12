package com.porvida.views

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.porvida.R
import com.porvida.AppDatabase
import com.porvida.data.repos.TrainingRepository
import com.porvida.models.MuscleGroup
import com.porvida.models.TrainingRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class TrainingRecordsActivity : AppCompatActivity() {
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
                RecordsScreen(userId, repo)
            }
        }
    }
}

@Composable
private fun RecordsScreen(userId: String, repo: TrainingRepository) {
    val scope = rememberCoroutineScope()
    var exercise by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") } // mm:ss
    var message by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(MuscleGroup.PECTORALES) }
    var records by remember { mutableStateOf(listOf<TrainingRecord>()) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var showUpper by remember { mutableStateOf(false) }
    var showLower by remember { mutableStateOf(false) }
    var showCardio by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        val ctx = LocalContext.current
        Text(ctx.getString(R.string.records_title), color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        // Category dropdowns
        CategoryDropdowns(
            showUpper = showUpper, onToggleUpper = { showUpper = !showUpper },
            showLower = showLower, onToggleLower = { showLower = !showLower },
            showCardio = showCardio, onToggleCardio = { showCardio = !showCardio },
            onPickGroup = { picked -> group = picked },
            onPickExercise = { name -> if (name != null) exercise = name else exercise = "" }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = exercise, onValueChange = { exercise = it }, label = { Text(ctx.getString(R.string.records_exercise)) }, textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface))
        Spacer(Modifier.height(8.dp))
        if (group == MuscleGroup.CARDIO || group == MuscleGroup.ABDOMINALES) {
            OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text(ctx.getString(R.string.records_time)) }, textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface))
        } else {
            OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text(ctx.getString(R.string.records_weight)) }, textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text(ctx.getString(R.string.records_reps)) }, textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface))
        }
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.height(12.dp))
        Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), onClick = {
            scope.launch(Dispatchers.IO) {
                val w = weight.toFloatOrNull()
                val r = reps.toIntOrNull()
                val ts = time.split(":").let { parts -> if (parts.size == 2) (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0) else null }
                repo.save(TrainingRecord(UUID.randomUUID().toString(), userId, group, exercise, w, r, ts))
                val newList = repo.listAll(userId)
                withContext(Dispatchers.Main) {
                    records = newList
                    message = ctx.getString(R.string.saved_ok)
                }
            }
        }) { Text(ctx.getString(R.string.records_save), color = Color.White) }
        Spacer(Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurface)

        // Records list
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(records) { rec ->
                RecordRow(
                    record = rec,
                    isEditing = editingId == rec.id,
                    onEdit = { editingId = rec.id },
                    onCancel = { editingId = null },
                    onDelete = {
                        scope.launch(Dispatchers.IO) {
                            repo.delete(rec.id)
                            val newList = repo.listAll(userId)
                            withContext(Dispatchers.Main) { records = newList }
                        }
                    },
                    onSave = { newWeight, newReps, newTime ->
                        scope.launch(Dispatchers.IO) {
                            val updated = rec.copy(
                                bestWeightKg = newWeight,
                                reps = newReps,
                                timeSeconds = newTime,
                                updatedAt = System.currentTimeMillis()
                            )
                            repo.save(updated)
                            val newList = repo.listAll(userId)
                            withContext(Dispatchers.Main) {
                                records = newList
                                editingId = null
                            }
                        }
                    }
                )
            }
        }

        // Initial load
        LaunchedEffect(userId) {
            records = withContext(Dispatchers.IO) { repo.listAll(userId) }
        }
    }
}

@Composable
private fun CategoryDropdowns(
    showUpper: Boolean,
    onToggleUpper: () -> Unit,
    showLower: Boolean,
    onToggleLower: () -> Unit,
    showCardio: Boolean,
    onToggleCardio: () -> Unit,
    onPickGroup: (MuscleGroup) -> Unit,
    onPickExercise: (String?) -> Unit
) {
    val ctx = LocalContext.current
    val buttonColors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            Button(colors = buttonColors, onClick = onToggleUpper) { Text(ctx.getString(R.string.cat_upper), color = MaterialTheme.colorScheme.primary) }
            DropdownMenu(expanded = showUpper, onDismissRequest = onToggleUpper) {
                DropdownMenuItem(text = { Text(ctx.getString(R.string.upper_hombro)) }, onClick = { onPickGroup(MuscleGroup.HOMBRO); onPickExercise(null); onToggleUpper() })
                DropdownMenuItem(text = { Text(ctx.getString(R.string.upper_pectorales)) }, onClick = { onPickGroup(MuscleGroup.PECTORALES); onPickExercise(null); onToggleUpper() })
                DropdownMenuItem(text = { Text(ctx.getString(R.string.upper_brazos)) }, onClick = { onPickGroup(MuscleGroup.BRAZOS); onPickExercise(null); onToggleUpper() })
                DropdownMenuItem(text = { Text(ctx.getString(R.string.upper_espalda)) }, onClick = { onPickGroup(MuscleGroup.ESPALDA); onPickExercise(null); onToggleUpper() })
            }
        }
        Box {
            Button(colors = buttonColors, onClick = onToggleLower) { Text(ctx.getString(R.string.cat_lower), color = MaterialTheme.colorScheme.primary) }
            DropdownMenu(expanded = showLower, onDismissRequest = onToggleLower) {
                DropdownMenuItem(text = { Text(ctx.getString(R.string.lower_gluteos)) }, onClick = { onPickGroup(MuscleGroup.GLUTEO); onPickExercise(null); onToggleLower() })
                DropdownMenuItem(text = { Text(ctx.getString(R.string.lower_cuadriceps)) }, onClick = { onPickGroup(MuscleGroup.CUADRICEPS); onPickExercise(null); onToggleLower() })
                DropdownMenuItem(text = { Text(ctx.getString(R.string.lower_femorales)) }, onClick = { onPickGroup(MuscleGroup.FEMORALES); onPickExercise(null); onToggleLower() })
                DropdownMenuItem(text = { Text(ctx.getString(R.string.lower_pantorrillas)) }, onClick = { onPickGroup(MuscleGroup.PANTORRILLAS); onPickExercise(null); onToggleLower() })
            }
        }
        Button(colors = buttonColors, onClick = { onPickGroup(MuscleGroup.ABDOMINALES); onPickExercise(null) }) { Text(ctx.getString(R.string.cat_abs), color = MaterialTheme.colorScheme.primary) }
        Box {
            Button(colors = buttonColors, onClick = onToggleCardio) { Text(ctx.getString(R.string.cat_cardio), color = MaterialTheme.colorScheme.primary) }
            DropdownMenu(expanded = showCardio, onDismissRequest = onToggleCardio) {
                DropdownMenuItem(text = { Text(ctx.getString(R.string.cardio_remadora)) }, onClick = { onPickGroup(MuscleGroup.CARDIO); onPickExercise(ctx.getString(R.string.cardio_remadora)); onToggleCardio() })
                DropdownMenuItem(text = { Text(ctx.getString(R.string.cardio_trotadora)) }, onClick = { onPickGroup(MuscleGroup.CARDIO); onPickExercise(ctx.getString(R.string.cardio_trotadora)); onToggleCardio() })
                DropdownMenuItem(text = { Text(ctx.getString(R.string.cardio_bici)) }, onClick = { onPickGroup(MuscleGroup.CARDIO); onPickExercise(ctx.getString(R.string.cardio_bici)); onToggleCardio() })
                DropdownMenuItem(text = { Text(ctx.getString(R.string.cardio_cuerda)) }, onClick = { onPickGroup(MuscleGroup.CARDIO); onPickExercise(ctx.getString(R.string.cardio_cuerda)); onToggleCardio() })
                DropdownMenuItem(text = { Text(ctx.getString(R.string.cardio_jumping)) }, onClick = { onPickGroup(MuscleGroup.CARDIO); onPickExercise(ctx.getString(R.string.cardio_jumping)); onToggleCardio() })
                DropdownMenuItem(text = { Text(ctx.getString(R.string.cardio_burpees)) }, onClick = { onPickGroup(MuscleGroup.CARDIO); onPickExercise(ctx.getString(R.string.cardio_burpees)); onToggleCardio() })
            }
        }
    }
}

@Composable
private fun RecordRow(
    record: TrainingRecord,
    isEditing: Boolean,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Float?, Int?, Int?) -> Unit
) {
    val ctx = LocalContext.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (!isEditing) {
            Text("${record.exerciseName} - ${record.muscleGroup}", color = onSurface)
            val detail = when {
                record.timeSeconds != null ->
                    ctx.getString(R.string.records_time) + ": " + secondsToMmSs(record.timeSeconds)
                record.bestWeightKg != null ->
                    ctx.getString(R.string.records_weight) + ": ${record.bestWeightKg} kg" + (record.reps?.let { ", ${ctx.getString(R.string.records_reps)}: $it" } ?: "")
                else -> ""
            }
            Text(detail, color = onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)), onClick = onEdit) { Text(ctx.getString(R.string.records_edit), color = MaterialTheme.colorScheme.primary) }
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)), onClick = onDelete) { Text(ctx.getString(R.string.records_delete), color = MaterialTheme.colorScheme.primary) }
            }
        } else {
            var w by remember { mutableStateOf(record.bestWeightKg?.toString() ?: "") }
            var r by remember { mutableStateOf(record.reps?.toString() ?: "") }
            var t by remember { mutableStateOf(record.timeSeconds?.let { secondsToMmSs(it) } ?: "") }
            if (record.timeSeconds != null && record.bestWeightKg == null) {
                OutlinedTextField(value = t, onValueChange = { t = it }, label = { Text(ctx.getString(R.string.records_time)) })
            } else {
                OutlinedTextField(value = w, onValueChange = { w = it }, label = { Text(ctx.getString(R.string.records_weight)) })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = r, onValueChange = { r = it }, label = { Text(ctx.getString(R.string.records_reps)) })
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), onClick = {
                    val newW = w.toFloatOrNull()
                    val newR = r.toIntOrNull()
                    val newT = t.split(":").let { p -> if (p.size == 2) (p[0].toIntOrNull() ?: 0) * 60 + (p[1].toIntOrNull() ?: 0) else null }
                    onSave(newW, newR, newT)
                }) { Text(ctx.getString(R.string.records_update), color = Color.White) }
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)), onClick = onCancel) { Text(ctx.getString(R.string.records_cancel), color = Color.White) }
            }
        }
    }
}

private fun secondsToMmSs(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}
