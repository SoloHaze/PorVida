package com.porvida

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.porvida.data.repos.UserRepository
import com.porvida.ui.login.RegisterActivity
import com.porvida.views.ClientDashboardActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var userRepository: UserRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar base de datos y repository
        val database = AppDatabase.getDatabase(this)
        userRepository = UserRepository(database.userDao())

        val etEmail = findViewById<EditText?>(R.id.etEmail)
        val etPassword = findViewById<EditText?>(R.id.etPassword)
        val btnLogin = findViewById<Button?>(R.id.btnLogin)
        val btnRegister = findViewById<Button?>(R.id.btnRegister)

        if (etEmail == null || etPassword == null || btnLogin == null || btnRegister == null) {
            Toast.makeText(this, "Layout de login incompleto: faltan campos", Toast.LENGTH_LONG).show()
            return
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            
            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            val user = userRepository.loginUser(email, password)
            if (user != null) {
                Toast.makeText(this@MainActivity, "Bienvenido ${user.name}", Toast.LENGTH_SHORT).show()
                
                // Navegar al dashboard correspondiente según el rol
                when (user.role) {
                    "CLIENT" -> {
                        val intent = Intent(this@MainActivity, ClientDashboardActivity::class.java)
                        intent.putExtra("userId", user.id)
                        intent.putExtra("userName", user.name)
                        startActivity(intent)
                        finish()
                    }
                    "ADMIN", "TECHNICIAN" -> {
                        // TODO: Implementar dashboard de administrador/técnico
                        Toast.makeText(this@MainActivity, "Dashboard de administrador no implementado aún", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this@MainActivity, "Email o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}