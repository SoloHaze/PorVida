package com.porvida.views

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

enum class DrawerDestination(val label: String, val route: String) {
    MisPlanes("Mis Planes", "mis_planes"),
    CambiarPlan("Cambiar/Adquirir Plan", "cambiar_plan"),
    Sedes("Nuestras Sedes", "sedes"),
    Pagos("Historial de Pagos", "pagos"),
    Acompanantes("Acompañantes", "companions"),
    Logout("Salir", "logout"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerDashboard(userName: String, userId: String) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    var selected by remember { mutableStateOf(DrawerDestination.MisPlanes) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(text = "Hola, $userName")
                fun closeDrawer() = scope.launch { drawerState.close() }

                NavigationDrawerItem(
                    label = { Text(DrawerDestination.MisPlanes.label) },
                    selected = selected == DrawerDestination.MisPlanes,
                    onClick = {
                        selected = DrawerDestination.MisPlanes
                        navController.navigate(DrawerDestination.MisPlanes.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                        }
                        closeDrawer()
                    },
                    icon = { Icon(Icons.Outlined.Checklist, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text(DrawerDestination.CambiarPlan.label) },
                    selected = selected == DrawerDestination.CambiarPlan,
                    onClick = {
                        context.startActivity(
                            Intent(context, PlanesActivity::class.java)
                                .putExtra("userId", userId)
                                .putExtra("userName", userName)
                        )
                        closeDrawer()
                    },
                    icon = { Icon(Icons.Outlined.Upgrade, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text(DrawerDestination.Sedes.label) },
                    selected = selected == DrawerDestination.Sedes,
                    onClick = {
                        context.startActivity(Intent(context, SedesActivity::class.java))
                        closeDrawer()
                    },
                    icon = { Icon(Icons.Outlined.Map, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text(DrawerDestination.Pagos.label) },
                    selected = selected == DrawerDestination.Pagos,
                    onClick = {
                        selected = DrawerDestination.Pagos
                        navController.navigate(DrawerDestination.Pagos.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                        }
                        closeDrawer()
                    },
                    icon = { Icon(Icons.Outlined.Payment, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text(DrawerDestination.Acompanantes.label) },
                    selected = selected == DrawerDestination.Acompanantes,
                    onClick = {
                        selected = DrawerDestination.Acompanantes
                        navController.navigate(DrawerDestination.Acompanantes.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                        }
                        closeDrawer()
                    },
                    icon = { Icon(Icons.Outlined.Group, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text(DrawerDestination.Logout.label) },
                    selected = false,
                    onClick = {
                        context.startActivity(
                            Intent(context, com.porvida.MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        closeDrawer()
                    },
                    icon = { Icon(Icons.Outlined.Logout, contentDescription = null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PorVida") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menú")
                        }
                    }
                )
            }
        ) { _ ->
            NavHost(
                navController = navController,
                startDestination = DrawerDestination.MisPlanes.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(DrawerDestination.MisPlanes.route) { Text("Tus planes contratados aparecerán aquí.") }
                composable(DrawerDestination.Pagos.route) { Text("Tus pagos aparecerán aquí.") }
                composable(DrawerDestination.Acompanantes.route) {
                    // Placeholder retained; real navigation handled in ClientDashboardActivity button.
                    Text("Tus acompañantes aparecerán aquí.")
                }
            }
        }
    }
}