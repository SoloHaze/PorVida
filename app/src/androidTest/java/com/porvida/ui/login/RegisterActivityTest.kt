package com.porvida.ui.login

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.hamcrest.CoreMatchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.porvida.R
import com.porvida.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

@RunWith(AndroidJUnit4::class)
class RegisterActivityTest {
    @get:Rule
    val activityRule = ActivityTestRule(RegisterActivity::class.java)

    @Test
    fun registrar_usuario_pendiente_crea_usuario_en_bd() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val db = AppDatabase.getDatabase(context)
        val email = "test.pendiente@example.com"
        val nombre = "Usuario Pendiente"
        val password = "secret123"

        // Asegurar limpieza previa
        runBlocking { db.userDao().findByEmail(email)?.let { db.userDao().deleteUser(it) } }

        // Rellenar campos
        onView(withId(R.id.etName)).perform(typeText(nombre), closeSoftKeyboard())
        onView(withId(R.id.etEmail)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.etConfirmPassword)).perform(typeText(password), closeSoftKeyboard())

        // Seleccionar plan "Pagar Más adelante"
        onView(withId(R.id.spinnerPlans)).perform(click())
        onData(allOf(`is`(instanceOf(String::class.java)), `is`("Pagar Más adelante"))).perform(click())

        // Enviar registro
        onView(withId(R.id.btnRegister)).perform(click())

        // Esperar a que coroutine termine
        Thread.sleep(1200)

        // Verificar usuario creado con plan PENDIENTE
        val created = runBlocking { db.userDao().findByEmail(email) }
        assertNotNull("Usuario no se insertó", created)
        assertEquals("PENDIENTE", created!!.plan)
    }
}
