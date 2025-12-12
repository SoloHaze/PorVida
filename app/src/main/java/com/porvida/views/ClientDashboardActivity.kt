package com.porvida.views

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalContext
import com.porvida.R
import com.porvida.AppDatabase
import com.porvida.MainActivity
import com.porvida.databinding.ActivityClientDashboardBinding
import com.porvida.data.repos.CompanionRepository
import com.porvida.data.repos.ServiceOrderRepository
import com.porvida.views.CheckoutActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = intent.getStringExtra("userId")
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "No se recibió el usuario", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val userName = intent.getStringExtra("userName") ?: "Cliente"

        val database = AppDatabase.getDatabase(this)
        val serviceOrderRepository = ServiceOrderRepository(database.serviceOrderDao())
    val companionRepository = CompanionRepository(database.companionDao(), database.userDao())

        setContent {
            // Tema oscuro + rojo para ítems del drawer
            val colorScheme = darkColorScheme(
                primary = Color(0xFFE53935),         // rojo para iconos/textos del menú
                background = Color(0xFF121212),      // fondo oscuro
                surface = Color(0xFF121212),
                onPrimary = Color.White,
                onBackground = Color(0xFFEDEDED),
                onSurface = Color(0xFFEDEDED)
            )
            MaterialTheme(colorScheme = colorScheme) {
                DashboardWithDrawer(
                    userId = userId,
                    userName = userName,
                    database = database,
                    serviceOrderRepository = serviceOrderRepository,
                    companionRepository = companionRepository,
                    activity = this
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardWithDrawer(
    userId: String,
    userName: String,
    database: AppDatabase,
    serviceOrderRepository: ServiceOrderRepository,
    companionRepository: CompanionRepository,
    activity: AppCompatActivity
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val red = MaterialTheme.colorScheme.primary

    val drawerItemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = Color.Transparent,
        unselectedContainerColor = Color.Transparent,
        selectedIconColor = red,
        selectedTextColor = red,
        unselectedIconColor = red,
        unselectedTextColor = red
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Text(text = "Hola, $userName")

                NavigationDrawerItem(
                    label = { Text("Mis Planes") },
                    selected = false,
                    onClick = {
                        activity.startActivity(
                            Intent(activity, MyPlansActivity::class.java)
                                .putExtra("userId", userId)
                                .putExtra("userName", userName)
                        )
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Checklist, contentDescription = null, tint = red) },
                    colors = drawerItemColors
                )
                NavigationDrawerItem(
                    label = { Text("Cambiar / Adquirir Plan") },
                    selected = false,
                    onClick = {
                        activity.startActivity(
                            Intent(activity, PlanesActivity::class.java)
                                .putExtra("userId", userId)
                                .putExtra("userName", userName)
                        )
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Upgrade, contentDescription = null, tint = red) },
                    colors = drawerItemColors
                )
                NavigationDrawerItem(
                    label = { Text("Nuestras Sedes") },
                    selected = false,
                    onClick = {
                        activity.startActivity(Intent(activity, SedesActivity::class.java))
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Map, contentDescription = null) },
                    colors = drawerItemColors
                )
                NavigationDrawerItem(
                    label = { Text(activity.getString(R.string.drawer_payments)) },
                    selected = false,
                    onClick = {
                        activity.startActivity(
                            Intent(activity, PaymentsActivity::class.java)
                                .putExtra("userId", userId)
                        )
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Payment, contentDescription = null) },
                    colors = drawerItemColors
                )
                NavigationDrawerItem(
                    label = { Text(activity.getString(R.string.drawer_companions)) },
                    selected = false,
                    onClick = {
                        activity.startActivity(
                            Intent(activity, CompanionsActivity::class.java)
                                .putExtra("userId", userId)
                                .putExtra("userName", userName)
                        )
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Group, contentDescription = null) },
                    colors = drawerItemColors
                )
                NavigationDrawerItem(
                    label = { Text(activity.getString(R.string.drawer_classes)) },
                    selected = false,
                    onClick = {
                        val i = Intent(activity, ClassesActivity::class.java).apply {
                            putExtra("userId", userId)
                            putExtra("sedeId", "sede_concha_toro")
                        }
                        activity.startActivity(i)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Checklist, contentDescription = null, tint = red) },
                    colors = drawerItemColors
                )
                NavigationDrawerItem(
                    label = { Text(activity.getString(R.string.drawer_chatbot)) },
                    selected = false,
                    onClick = {
                        val i = Intent(activity, AssistantActivity::class.java).apply { putExtra("userId", userId) }
                        activity.startActivity(i)
                        scope.launch { drawerState.close() }
                    },
                    icon = {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.porvida.R.drawable.ic_chatbot),
                            contentDescription = null
                        )
                    },
                    colors = drawerItemColors
                )
                NavigationDrawerItem(
                    label = { Text("Chatbot (IA)") },
                    selected = false,
                    onClick = {
                        val i = Intent(activity, ChatbotActivity::class.java).apply { putExtra("userId", userId) }
                        activity.startActivity(i)
                        scope.launch { drawerState.close() }
                    },
                    icon = {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.porvida.R.drawable.ic_chatbot),
                            contentDescription = null
                        )
                    },
                    colors = drawerItemColors
                )
                NavigationDrawerItem(
                    label = { Text(activity.getString(R.string.drawer_records)) },
                    selected = false,
                    onClick = {
                        val i = Intent(activity, TrainingRecordsActivity::class.java).apply { putExtra("userId", userId) }
                        activity.startActivity(i)
                        scope.launch { drawerState.close() }
                    },
                    icon = {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.porvida.R.drawable.ic_kg_red),
                            contentDescription = null
                        )
                    },
                    colors = drawerItemColors
                )
                NavigationDrawerItem(
                    label = { Text("Notas/Calendario") },
                    selected = false,
                    onClick = {
                        val i = Intent(activity, NotesActivity::class.java).apply { putExtra("userId", userId) }
                        activity.startActivity(i)
                        scope.launch { drawerState.close() }
                    },
                    icon = {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.porvida.R.drawable.ic_calendar_mini),
                            contentDescription = null
                        )
                    },
                    colors = drawerItemColors
                )
                NavigationDrawerItem(
                    label = { Text("Cámara / QR") },
                    selected = false,
                    onClick = {
                        val i = Intent(activity, CameraActivity::class.java)
                        activity.startActivity(i)
                        scope.launch { drawerState.close() }
                    },
                    icon = {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.porvida.R.drawable.ic_camera),
                            contentDescription = null
                        )
                    },
                    colors = drawerItemColors
                )
                NavigationDrawerItem(
                    label = { Text("Clima / Tiempo") },
                    selected = false,
                    onClick = {
                        val i = Intent(activity, WeatherActivity::class.java)
                        activity.startActivity(i)
                        scope.launch { drawerState.close() }
                    },
                    icon = {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.porvida.R.drawable.ic_weather),
                            contentDescription = null
                        )
                    },
                    colors = drawerItemColors
                )
                NavigationDrawerItem(
                    label = { Text("Salir") },
                    selected = false,
                    onClick = {
                        activity.startActivity(
                            Intent(activity, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Outlined.Logout, contentDescription = null) },
                    colors = drawerItemColors
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PorVida") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = red
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menú")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    val i = android.content.Intent(activity, ChatbotActivity::class.java).apply { putExtra("userId", userId) }
                    activity.startActivity(i)
                }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
                    Icon(Icons.Outlined.SmartToy, contentDescription = "Chatbot")
                }
            }
        ) { _ ->
            // Extraemos los colores del tema AQUÍ (contexto composable) y sólo pasamos ints al binding.
            // Usar el color primario real para el fondo de botones y onPrimary para el texto
            val primaryInt = MaterialTheme.colorScheme.background.toArgb()
            val onPrimaryInt = MaterialTheme.colorScheme.primary.toArgb()
            AndroidViewBinding(
                factory = { inflater, parent, attachToParent ->
                    ActivityClientDashboardBinding.inflate(inflater, parent, attachToParent)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                tvWelcome.text = "Bienvenido, $userName"
                tvWelcome.setTextColor(onPrimaryInt)

                // Estilo diferenciado para estos dos botones (fondo rojo, texto blanco)
                fun styleMainButtons() {
                    // Tomar color y grosor de borde de un botón de referencia (btnPayments) para uniformidad
                    val refMaterial = (btnPayments as? com.google.android.material.button.MaterialButton)
                    val refStrokeColor = refMaterial?.strokeColor
                        ?: ColorStateList.valueOf(android.graphics.Color.argb(120, 255, 255, 255)) // fallback gris/blanco suave
                    val refStrokeWidth = refMaterial?.strokeWidth
                        ?: (2 * root.resources.displayMetrics.density).toInt()

                    val redTint = ColorStateList.valueOf(onPrimaryInt) // onPrimaryInt representa el rojo en tu configuración actual

                    listOf(btnMyServices, btnCompanions, btnNewService, btnClasses, btnChatbot, btnRecords, btnCamera).forEach { b ->
                        // extended list includes weather button if present
                        b.backgroundTintList = ColorStateList.valueOf(primaryInt)
                        b.setTextColor(onPrimaryInt)
                        (b as? com.google.android.material.button.MaterialButton)?.apply {
                            strokeWidth = refStrokeWidth
                            strokeColor = refStrokeColor
                            iconTint = redTint
                        }
                        // Si son AppCompatButton con drawables compuestos
                        b.compoundDrawableTintList = redTint
                    }
                    // Style weather button separately if it exists
                    btnWeather?.apply {
                        backgroundTintList = ColorStateList.valueOf(primaryInt)
                        setTextColor(onPrimaryInt)
                        (this as? com.google.android.material.button.MaterialButton)?.apply {
                            strokeWidth = (2 * root.resources.displayMetrics.density).toInt()
                            strokeColor = ColorStateList.valueOf(android.graphics.Color.argb(120,255,255,255))
                            iconTint = ColorStateList.valueOf(onPrimaryInt)
                        }
                        compoundDrawableTintList = ColorStateList.valueOf(onPrimaryInt)
                    }
                }
                styleMainButtons()

                activity.lifecycleScope.launch {
                    serviceOrderRepository.getOrdersByUserId(userId).collectLatest { orders ->
                        val total = orders.size
                        val pendientes = orders.count { it.status == "PENDING" }
                        // Si no hay órdenes registradas pero el usuario tiene un plan asignado, mostramos el plan en lugar de 0/0
                        activity.lifecycleScope.launch(Dispatchers.IO) {
                            val user = database.userDao().getUserById(userId)
                            withContext(Dispatchers.Main) {
                                if (total == 0 && user?.plan != null) {
                                    btnMyServices.text = "Mi Plan (${user.plan})"
                                } else {
                                    btnMyServices.text = "Mis Planes ($pendientes / $total)"
                                }
                                btnMyServices.setTextColor(onPrimaryInt)
                            }
                        }
                    }
                }

                activity.lifecycleScope.launch(Dispatchers.IO) {
                    val count = companionRepository.getActiveCompanionCount(userId)
                    withContext(Dispatchers.Main) {
                        btnCompanions.text = "Acompañantes ($count/3)"
                        btnCompanions.setTextColor(onPrimaryInt)
                    }
                }

                // Mostrar botón "Pagar plan" según vigencia y ventana 1–10 del mes siguiente
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    val user = database.userDao().getUserById(userId)
                    val lastPayment = database.paymentDao().getLastCompletedPayment(userId)
                    val plan = user?.plan ?: "BASICO"
                    val price = when (plan.uppercase()) {
                        "BASICO" -> 13500
                        "BLACK" -> 19500
                        "ULTRA" -> 23500
                        else -> 13500
                    }
                    val now = java.util.Calendar.getInstance()
                    val nowMillis = System.currentTimeMillis()
                    val validUntil = user?.planValidUntil ?: 0L

                    // calcular si estamos en ventana de renovación (1–10 del mes siguiente a la fecha de expiración)
                    var inRenewalWindow = false
                    if (validUntil > 0L) {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = validUntil }
                        // mover al primer día del mes siguiente
                        cal.add(java.util.Calendar.MONTH, 1)
                        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        val windowStart = cal.timeInMillis
                        cal.set(java.util.Calendar.DAY_OF_MONTH, 10)
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                        cal.set(java.util.Calendar.MINUTE, 59)
                        cal.set(java.util.Calendar.SECOND, 59)
                        cal.set(java.util.Calendar.MILLISECOND, 999)
                        val windowEnd = cal.timeInMillis
                        inRenewalWindow = nowMillis in windowStart..windowEnd
                    }

                    val isExpired = validUntil == 0L || nowMillis > validUntil
                    val isPending = plan.equals("PENDIENTE", ignoreCase = true)
                    val shouldShowPay = isPending || lastPayment == null || isExpired || inRenewalWindow
                    withContext(Dispatchers.Main) {
                        btnPayments.text = if (shouldShowPay) {
                            if (isPending) "Pagar plan" else "Pagar plan ($plan $${price.toString().reversed().chunked(3).joinToString(".").reversed()})"
                        } else "Pagos"
                        btnPayments.setOnClickListener {
                            if (shouldShowPay) {
                                if (isPending) {
                                    val i = Intent(root.context, PlanesActivity::class.java).apply {
                                        putExtra("userId", userId)
                                        putExtra("userName", userName)
                                    }
                                    root.context.startActivity(i)
                                } else {
                                    val i = Intent(root.context, CheckoutActivity::class.java).apply {
                                        putExtra("plan", plan)
                                        putExtra("price", price)
                                        putExtra("userId", userId)
                                    }
                                    root.context.startActivity(i)
                                }
                            } else {
                                val i = Intent(root.context, PaymentsActivity::class.java).apply {
                                    putExtra("userId", userId)
                                }
                                root.context.startActivity(i)
                            }
                        }
                    }
                }

                btnMyServices.setOnClickListener {
                    val i = Intent(root.context, MyPlansActivity::class.java).apply {
                        putExtra("userId", userId)
                        putExtra("userName", userName)
                    }
                    root.context.startActivity(i)
                }
                btnNewService.setOnClickListener {
                    val i = Intent(root.context, PlanesActivity::class.java).apply {
                        putExtra("userId", userId)
                    }
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        val user = database.userDao().getUserById(userId)
                        withContext(Dispatchers.Main) {
                            i.putExtra("currentPlan", user?.plan)
                            root.context.startActivity(i)
                        }
                    }
                }
                // onClick reasignado arriba dinámicamente según estado de pago
                btnCompanions.setOnClickListener {
                    val intent = Intent(root.context, CompanionsActivity::class.java)
                    intent.putExtra("userId", userId)
                    intent.putExtra("userName", userName)
                    root.context.startActivity(intent)
                }
                btnSedes.setOnClickListener {
                    root.context.startActivity(Intent(root.context, SedesActivity::class.java))
                }
                btnClasses.setOnClickListener {
                    val i = Intent(root.context, ClassesActivity::class.java).apply {
                        putExtra("userId", userId)
                        putExtra("sedeId", "sede_concha_toro")
                    }
                    root.context.startActivity(i)
                }
                btnChatbot.setOnClickListener {
                    val i = Intent(root.context, AssistantActivity::class.java).apply { putExtra("userId", userId) }
                    root.context.startActivity(i)
                }
                btnRecords.setOnClickListener {
                    val i = Intent(root.context, TrainingRecordsActivity::class.java).apply { putExtra("userId", userId) }
                    root.context.startActivity(i)
                }
                btnCamera.setOnClickListener {
                    val i = Intent(root.context, CameraActivity::class.java)
                    root.context.startActivity(i)
                }
                btnWeather?.setOnClickListener {
                    val i = Intent(root.context, WeatherActivity::class.java)
                    root.context.startActivity(i)
                }
                // New Notes/Calendar button
                btnNotes.setOnClickListener {
                    val i = Intent(root.context, NotesActivity::class.java).apply { putExtra("userId", userId) }
                    root.context.startActivity(i)
                }
                btnLogout.setOnClickListener {
                    val i = Intent(root.context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    root.context.startActivity(i)
                    activity.finish()
                }
            }
        }
    }
}