package com.porvida.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.porvida.AppDatabase
import com.porvida.R
import com.porvida.data.repos.UserRepository
import kotlinx.coroutines.launch

class PlanesActivity : AppCompatActivity() {
    private lateinit var userRepository: UserRepository
    private var userId: String? = null
    private var currentPlan: String? = null
    private var pendingPlan: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planes)

        userId = intent.getStringExtra("userId")
        currentPlan = intent.getStringExtra("currentPlan")

        val db = AppDatabase.getDatabase(this)
        userRepository = UserRepository(db.userDao())

        val tvCurrentPlan = findViewById<TextView>(R.id.tvCurrentPlan)
        val btnBasico = findViewById<Button>(R.id.btnPlanBasico)
        val btnBlack = findViewById<Button>(R.id.btnPlanBlack)
        val btnUltra = findViewById<Button>(R.id.btnPlanUltra)
        val btnClose = findViewById<Button>(R.id.btnClosePlanes)

        tvCurrentPlan.text = "Tu plan actual: ${currentPlan ?: "Desconocido"}"

        fun changePlan(target: String) {
            val uid = userId ?: return
            if (currentPlan == target) {
                Toast.makeText(this, "Ya tienes el plan $target", Toast.LENGTH_SHORT).show()
                return
            }
            // Requerir pago antes de actualizar
            pendingPlan = target
            val price = when (target.uppercase()) {
                "BASICO" -> 13500
                "BLACK" -> 19500
                "ULTRA" -> 23500
                else -> 13500
            }
            val intent = Intent(this@PlanesActivity, CheckoutActivity::class.java)
            intent.putExtra("plan", target)
            intent.putExtra("price", price)
            intent.putExtra("userId", userId)
            startActivityForResult(intent, 2002)
        }

        btnBasico.setOnClickListener { changePlan("BASICO") }
        btnBlack.setOnClickListener { changePlan("BLACK") }
        btnUltra.setOnClickListener { changePlan("ULTRA") }
        btnClose.setOnClickListener { finish() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2002 && resultCode == Activity.RESULT_OK) {
            val target = pendingPlan ?: return
            val uid = userId ?: return
            lifecycleScope.launch {
                val ok = userRepository.updateUserPlan(uid, target)
                if (ok) {
                    currentPlan = target
                    findViewById<TextView>(R.id.tvCurrentPlan)?.text = "Tu plan actual: $target"
                    Toast.makeText(this@PlanesActivity, "Plan actualizado a $target", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PlanesActivity, "No se pudo actualizar el plan", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}