package com.porvida.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.Spinner
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.porvida.AppDatabase
import com.porvida.R
import com.porvida.data.repos.UserRepository
import com.porvida.models.Payment
import kotlinx.coroutines.launch
import java.util.UUID

class RegisterActivity : AppCompatActivity() {
    private lateinit var userRepository: UserRepository
    private var pendingRegistration: Triple<String, String, Pair<String, String>>? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        // Inicializar base de datos y repository
        val database = AppDatabase.getDatabase(this)
        userRepository = UserRepository(database.userDao())
        
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        // Spinner de planes (BASICO, BLACK, ULTRA)
        val spinnerPlans = findViewById<Spinner?>(R.id.spinnerPlans)
        spinnerPlans?.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("BASICO", "BLACK", "+ULTRA", "Pagar Más adelante")
        )
        val btnBack = findViewById<Button>(R.id.btnBack)
        
        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()
            
            if (validateInput(name, email, password, confirmPassword)) {
                val selectedPlan = spinnerPlans?.selectedItem?.toString() ?: "BASICO"
                if (selectedPlan.equals("Pagar Más adelante", ignoreCase = true)) {
                    registerPendingUser(name, email, password)
                } else {
                    // Normalizar +ULTRA a ULTRA
                    val planValue = if (selectedPlan.startsWith("+")) selectedPlan.replace("+", "") else selectedPlan
                    // Guardar datos temporalmente y lanzar pago
                    pendingRegistration = Triple(name, email, planValue to password)
                    startCheckout(planValue)
                }
            }
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun startCheckout(plan: String) {
        val price = when (plan.uppercase()) {
            "BASICO" -> 13500
            "BLACK" -> 19500
            "ULTRA" -> 23500
            else -> 13500
        }
        val intent = Intent(this, com.porvida.views.CheckoutActivity::class.java)
        intent.putExtra("plan", plan)
        intent.putExtra("price", price)
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            if (resultCode == Activity.RESULT_OK) {
                val pending = pendingRegistration ?: return
                val name = pending.first
                val email = pending.second
                val plan = pending.third.first
                val password = pending.third.second
                registerUser(name, email, password, plan)
            } else {
                Toast.makeText(this, "Pago cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        when {
            name.isBlank() -> {
                Toast.makeText(this, "Por favor ingresa tu nombre", Toast.LENGTH_SHORT).show()
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "Por favor ingresa un email válido", Toast.LENGTH_SHORT).show()
                return false
            }
            password.length < 6 -> {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return false
            }
            password != confirmPassword -> {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return false
            }
            else -> return true
        }
    }
    
    private fun registerUser(name: String, email: String, password: String, plan: String) {
        lifecycleScope.launch {
            // Verificar si ya existe un usuario con ese email
            val existing = AppDatabase.getDatabase(this@RegisterActivity).userDao().findByEmail(email)
            if (existing != null) {
                Toast.makeText(this@RegisterActivity, "El email ya está registrado", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val now = System.currentTimeMillis()
            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
            val success = userRepository.addUser(name, email, password, plan = plan, planValidUntil = now + thirtyDaysMs)
            if (success) {
                // Insertar registro de pago COMPLETED para reflejar estado pagado
                val db = AppDatabase.getDatabase(this@RegisterActivity)
                val createdUser = db.userDao().findByEmail(email)
                createdUser?.let { u ->
                    val price = when (plan.uppercase()) {
                        "BASICO" -> 13500
                        "BLACK" -> 19500
                        "ULTRA" -> 23500
                        else -> 13500
                    }
                    val payment = Payment(
                        id = UUID.randomUUID().toString(),
                        userId = u.id,
                        orderId = "PLAN_${plan}_${System.currentTimeMillis()}",
                        amount = price.toDouble(),
                        paymentMethod = "GOOGLE_PAY",
                        status = "COMPLETED",
                        transactionId = null,
                        description = "Pago plan $plan"
                    )
                    db.paymentDao().insertPayment(payment)
                }
                Toast.makeText(this@RegisterActivity, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@RegisterActivity, "Error al registrar usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerPendingUser(name: String, email: String, password: String) {
        lifecycleScope.launch {
            // Verificar si ya existe un usuario con ese email
            val existing = AppDatabase.getDatabase(this@RegisterActivity).userDao().findByEmail(email)
            if (existing != null) {
                Toast.makeText(this@RegisterActivity, "El email ya está registrado", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val success = userRepository.addUser(name, email, password, plan = "PENDIENTE", planValidUntil = 0L)
            if (success) {
                Toast.makeText(this@RegisterActivity, "Cuenta creada. Podrás pagar más adelante.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this@RegisterActivity, "Error al registrar usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }
}