package com.porvida.views

import android.app.Activity
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.porvida.R
import com.porvida.AppDatabase
import com.porvida.models.Payment
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class CheckoutActivity : AppCompatActivity() {

    private lateinit var paymentsClient: PaymentsClient
    private lateinit var payButton: Button
    private val LOAD_PAYMENT_DATA_REQUEST_CODE = 991

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

    val plan = intent.getStringExtra("plan") ?: "BASICO"
    val price = intent.getIntExtra("price", 13500)
        findViewById<TextView>(R.id.tvPlanInfo)?.text = "Plan: $plan - $${price.toString().reversed().chunked(3).joinToString(".").reversed()}"

        paymentsClient = Wallet.getPaymentsClient(
            this,
            Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST).build()
        )

        // Botón simple para abrir la hoja de Google Pay. Evitamos el widget PayButton para
        // simplificar compatibilidad de dependencias. La lógica es la misma.
        payButton = findViewById(R.id.btnGooglePay)
        try {
            checkIsReadyToPay()
            payButton.setOnClickListener { requestPayment(price) }
        } catch (_: JSONException) { /* si falla JSON, dejamos el botón oculto */ }
    }

    private fun checkIsReadyToPay() {
        val request = getIsReadyToPayRequest() ?: return
        val task = paymentsClient.isReadyToPay(request)
        task.addOnCompleteListener { t ->
            val canPay = t.isSuccessful && (t.result == true)
            payButton.visibility = if (canPay) View.VISIBLE else View.GONE
            if (!canPay) Toast.makeText(this, "Google Pay no disponible", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPayment(amount: Int) {
        val task = paymentsClient.loadPaymentData(getPaymentDataRequest(amount) ?: return)
        task.addOnSuccessListener { paymentData ->
            // Pago completado: token disponible
            handlePaymentSuccess(paymentData)
        }
        task.addOnFailureListener { e ->
            // Si requiere resolución (por ejemplo, falta método de pago), disparamos el IntentSender
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this, LOAD_PAYMENT_DATA_REQUEST_CODE)
                } catch (sie: IntentSender.SendIntentException) {
                    handleError(CommonStatusCodes.ERROR, sie.localizedMessage)
                }
            } else {
                handleError(CommonStatusCodes.ERROR, e.localizedMessage)
            }
        }
    }

    private fun getAllowedPaymentMethods(): JSONArray = JSONArray().put(getBaseCardMethod())

    private fun handlePaymentSuccess(paymentData: PaymentData?) {
        try {
            val json = paymentData?.toJson() ?: return
            val pm = JSONObject(json).getJSONObject("paymentMethodData")
            Log.d("GooglePayToken", pm.getJSONObject("tokenizationData").getString("token"))
            // Persistir un pago completado si se proporcionó userId
            val userId = intent.getStringExtra("userId")
            val plan = intent.getStringExtra("plan") ?: "BASICO"
            val price = intent.getIntExtra("price", 13500)
            if (!userId.isNullOrEmpty()) {
                val db = AppDatabase.getDatabase(this)
                val payment = Payment(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    orderId = "PLAN_${plan}_${System.currentTimeMillis()}",
                    amount = price.toDouble(),
                    paymentMethod = "GOOGLE_PAY",
                    status = "COMPLETED",
                    transactionId = null,
                    description = "Pago plan $plan"
                )
                // Guardamos en Room desde un dispatcher de IO (las funciones del DAO son suspend)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        db.paymentDao().insertPayment(payment)
                        val user = db.userDao().getUserById(userId)
                        user?.let {
                            val base = maxOf(System.currentTimeMillis(), it.planValidUntil)
                            val extended = base + 30L * 24 * 60 * 60 * 1000
                            db.userDao().insertUser(it.copy(planValidUntil = extended))
                        }
                    } catch (_: Exception) { }
                }
            }
            Toast.makeText(this, "Pago realizado con éxito", Toast.LENGTH_LONG).show()
            setResult(Activity.RESULT_OK)
            finish()
        } catch (e: Exception) {
            Log.e("PaymentSuccess", "Error", e)
        }
    }

    private fun handleError(code: Int, message: String?) {
        Log.e("PaymentError", "Code: $code, Message: $message")
        Toast.makeText(this, "Error en el pago", Toast.LENGTH_SHORT).show()
    }

    private fun getIsReadyToPayRequest(): com.google.android.gms.wallet.IsReadyToPayRequest? {
        val base = JSONObject().put("apiVersion", 2).put("apiVersionMinor", 0)
        val methods = JSONArray().put(getBaseCardMethod())
        base.put("allowedPaymentMethods", methods)
        return com.google.android.gms.wallet.IsReadyToPayRequest.fromJson(base.toString())
    }

    private fun getBaseCardMethod(): JSONObject = JSONObject()
        .put("type", "CARD")
        .put(
            "parameters",
            JSONObject()
                .put("allowedCardNetworks", JSONArray(listOf("VISA", "MASTERCARD")))
                .put("allowedAuthMethods", JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS")))
        )

    private fun getCardMethodWithTokenization(): JSONObject = JSONObject()
        .put("type", "CARD")
        .put("tokenizationSpecification", JSONObject()
            .put("type", "PAYMENT_GATEWAY")
            .put("parameters", JSONObject()
                .put("gateway", "example")
                .put("gatewayMerchantId", "exampleGatewayMerchantId")
            )
        )
        .put(
            "parameters",
            JSONObject()
                .put("allowedCardNetworks", JSONArray(listOf("VISA", "MASTERCARD")))
                .put("allowedAuthMethods", JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS")))
                .put("billingAddressRequired", true)
                .put("billingAddressParameters", JSONObject().put("format", "FULL"))
        )

    private fun getPaymentDataRequest(amount: Int): com.google.android.gms.wallet.PaymentDataRequest? {
        val base = JSONObject().put("apiVersion", 2).put("apiVersionMinor", 0)
        val methods = JSONArray().put(getCardMethodWithTokenization())
        val transaction = JSONObject()
            .put("totalPrice", (amount.toBigDecimal().movePointLeft(0)).toString())
            .put("totalPriceStatus", "FINAL")
            .put("currencyCode", "CLP")
        val merchant = JSONObject().put("merchantName", "PorVida")
        base.put("allowedPaymentMethods", methods)
            .put("transactionInfo", transaction)
            .put("merchantInfo", merchant)
        return com.google.android.gms.wallet.PaymentDataRequest.fromJson(base.toString())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val paymentData = PaymentData.getFromIntent(data!!)
                    handlePaymentSuccess(paymentData)
                }
                else -> {
                    // Usuario canceló u ocurrió un error genérico
                    Toast.makeText(this, "Pago cancelado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
