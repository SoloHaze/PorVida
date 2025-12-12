package com.porvida.views

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.porvida.AppDatabase
import com.porvida.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SedesActivity : AppCompatActivity(), OnMapReadyCallback {
    private var googleMap: GoogleMap? = null
    private lateinit var container: LinearLayout
    private lateinit var btnClose: Button
    private val LOCATION_REQ_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sedes)

        container = findViewById(R.id.containerSedes)
        btnClose = findViewById(R.id.btnCloseSedes)

        // Inicializar fragmento de mapa
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

        btnClose.setOnClickListener { finish() }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        enableMyLocationIfPermitted()
        cargarSedes()
    }

    private fun enableMyLocationIfPermitted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQ_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQ_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationIfPermitted()
        }
    }

    private fun cargarSedes() {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            var sedes = db.sedeDao().getAllSedes().first()
            if (sedes.isEmpty()) {
                // Seed inicial con la sede solicitada
                val sede = com.porvida.models.Sede(
                    id = "sede_concha_toro",
                    name = "PorVida Concha y Toro",
                    address = "Av. Concha y Toro N°1340 esquina San Carlos",
                    city = "",
                    phone = "",
                    email = null,
                    latitude = -33.5984427182924,
                    longitude = -70.57880853188047,
                    workingHours = "{\"open\":\"06:30\",\"close\":\"23:00\"}",
                    services = "[\"Crossfit\",\"WorkBody\",\"Ritmo\",\"MachineFit\"]",
                    capacity = 200,
                    amenities = "[\"Estacionamiento\",\"Lockers\",\"Duchas\"]",
                    isActive = true
                )
                db.sedeDao().insertSede(sede)
                sedes = db.sedeDao().getAllSedes().first()
            }
            runOnUiThread {
                container.removeAllViews()
                if (sedes.isEmpty()) {
                    val tv = TextView(this@SedesActivity)
                    tv.text = "No hay sedes registradas todavía"
                    container.addView(tv)
                    // Centrar en una coordenada genérica (ej. Ciudad de México)
                    val center = LatLng(19.4326, -99.1332)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 4.5f))
                } else {
                    var firstLatLng: LatLng? = null
                    sedes.forEach { sede ->
                        val tv = TextView(this@SedesActivity)
                        tv.text = "• ${sede.name}${if (sede.city.isNotBlank()) " - ${sede.city}" else ""}\n${sede.address}\nHorario: 06:30 a 23:00"
                        tv.setPadding(0, 12, 0, 12)
                        tv.setOnClickListener {
                            // Si tenemos coordenadas, centra el mapa; si no, abre Google Maps con la dirección
                            if (sede.latitude != null && sede.longitude != null) {
                                val pos = LatLng(sede.latitude, sede.longitude)
                                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                            } else {
                                val uri = android.net.Uri.parse("geo:0,0?q=" + java.net.URLEncoder.encode(sede.address, "UTF-8"))
                                val i = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                i.setPackage("com.google.android.apps.maps")
                                startActivity(i)
                            }
                        }
                        container.addView(tv)
                        if (sede.latitude != null && sede.longitude != null) {
                            val pos = LatLng(sede.latitude, sede.longitude)
                            if (firstLatLng == null) firstLatLng = pos
                            googleMap?.addMarker(
                                MarkerOptions()
                                    .position(pos)
                                    .title(sede.name)
                                    .snippet(sede.address)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            )
                        }
                    }
                    firstLatLng?.let { googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 11f)) }
                }
            }
        }
    }
}