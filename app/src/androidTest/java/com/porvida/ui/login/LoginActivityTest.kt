package com.porvida.ui.login

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.Intents.intended
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.porvida.R
import com.porvida.AppDatabase
import com.porvida.data.repos.UserRepository
import com.porvida.views.ClientDashboardActivity
import kotlinx.coroutines.runBlocking
import at.favre.lib.crypto.bcrypt.BCrypt
import com.porvida.models.User
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {
    @get:Rule
    val activityRule = ActivityTestRule(com.porvida.MainActivity::class.java, true, false)

    @Test
    fun login_exitoso_navega_a_dashboard_cliente() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db = AppDatabase.getDatabase(context)
        val email = "login.tester@example.com"
        val password = "pass1234"
        val name = "Tester" 

        // Limpiar usuario previo si existe
        runBlocking { db.userDao().findByEmail(email)?.let { db.userDao().deleteUser(it) } }

        // Insertar usuario con password hasheado
        val hashed = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val user = User(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email,
            password = hashed,
            role = "CLIENT",
            plan = "BASICO",
            planValidUntil = System.currentTimeMillis() + 30L*24*60*60*1000,
            createdAt = System.currentTimeMillis()
        )
        runBlocking { db.userDao().insertUser(user) }

        // Lanzar actividad luego de preparar datos
        activityRule.launchActivity(null)

        // Rellenar credenciales
        onView(withId(R.id.etEmail)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())

        // Iniciar Intents captura
        Intents.init()
        try {
            onView(withId(R.id.btnLogin)).perform(click())
            // Esperar a coroutine y navegación
            Thread.sleep(1500)
            intended(hasComponent(ClientDashboardActivity::class.java.name))
        } finally {
            Intents.release()
        }
    }
}
