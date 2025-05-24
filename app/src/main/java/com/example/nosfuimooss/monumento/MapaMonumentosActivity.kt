package com.example.nosfuimooss.monumento

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.NosFuimooss.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.nosfuimooss.api.RetrofitClient
import com.example.nosfuimooss.model.Monumento
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapaMonumentosActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private var ubicacionDestino: String? = null
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar OSMDroid
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        setContentView(R.layout.activity_mapa_monumentos)

        // Obtener la ubicación del destino desde el Intent
        ubicacionDestino = intent.getStringExtra("ubicacionDestino")

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.hide()

        initializeMap()
        requestPermissionsIfNecessary(arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ))

        loadMonumentosForMap()
    }

    private fun initializeMap() {
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Configurar zoom inicial
        val mapController = map.controller
        mapController.setZoom(6.0)

        // Centrar en España por defecto
        val startPoint = GeoPoint(40.4168, -3.7038) // Madrid
        mapController.setCenter(startPoint)
    }

    private fun loadMonumentosForMap() {
        val call = if (!ubicacionDestino.isNullOrEmpty()) {
            RetrofitClient.monumentoApiService.getMonumentosByUbicacion(ubicacionDestino!!.trim())
        } else {
            RetrofitClient.monumentoApiService.getAllMonumentos()
        }

        call.enqueue(object : Callback<List<Monumento>> {
            override fun onResponse(call: Call<List<Monumento>>, response: Response<List<Monumento>>) {
                if (response.isSuccessful) {
                    val monumentos = response.body()
                    if (monumentos != null && monumentos.isNotEmpty()) {
                        addMonumentosToMap(monumentos)
                        // Centrar el mapa en el primer monumento si hay ubicación específica
                        if (!ubicacionDestino.isNullOrEmpty() && monumentos.isNotEmpty()) {
                            centerMapOnMonuments(monumentos)
                        }
                    } else {
                        Toast.makeText(this@MapaMonumentosActivity,
                            "No se encontraron monumentos para mostrar",
                            Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MapaMonumentosActivity,
                        "Error al cargar los monumentos",
                        Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Monumento>>, t: Throwable) {
                Toast.makeText(this@MapaMonumentosActivity,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addMonumentosToMap(monumentos: List<Monumento>) {
        for (monumento in monumentos) {
            // Verificar que el monumento tenga coordenadas válidas
            val lat = monumento.latitud
            val lon = monumento.longitud

            if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
                val marker = Marker(map)
                marker.position = GeoPoint(lat, lon)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = monumento.nombre ?: "Monumento"
                marker.snippet = "${monumento.informacion ?: "Sin información"}\nRating: ${monumento.estrella ?: "N/A"}"

                // Personalizar el icono del marcador si tienes uno específico
                try {
                    val drawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.marcador)
                    if (drawable != null) {
                        marker.icon = drawable
                    }
                } catch (e: Exception) {
                    // Usar el marcador por defecto si hay error
                }

                map.overlays.add(marker)
            }
        }

        // Refrescar el mapa
        map.invalidate()
    }

    private fun centerMapOnMonuments(monumentos: List<Monumento>) {
        val validMonuments = monumentos.filter {
            it.latitud != null && it.longitud != null &&
                    it.latitud != 0.0 && it.longitud != 0.0
        }

        if (validMonuments.isNotEmpty()) {
            if (validMonuments.size == 1) {
                // Si solo hay un monumento, centrarlo
                val monumento = validMonuments.first()
                val point = GeoPoint(monumento.latitud!!, monumento.longitud!!)
                map.controller.setCenter(point)
                map.controller.setZoom(15.0)
            } else {
                // Si hay múltiples monumentos, ajustar para mostrar todos
                val latitudes = validMonuments.mapNotNull { it.latitud }
                val longitudes = validMonuments.mapNotNull { it.longitud }

                val centerLat = latitudes.average()
                val centerLon = longitudes.average()

                map.controller.setCenter(GeoPoint(centerLat, centerLon))
                map.controller.setZoom(15.0)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSIONS_REQUEST_CODE -> {
                // Verificar si los permisos fueron concedidos
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permisos concedidos
                } else {
                    Toast.makeText(this, "Permisos necesarios para el mapa", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}