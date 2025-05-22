package com.example.nosfuimooss.hotel


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Hotel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


class MapaActivity : AppCompatActivity() {


    private lateinit var map: MapView
    private lateinit var progressBar: View
    private var adultos: Int = 1
    private var ninos: Int = 0
    private var destino: String = ""
    private var fechaEntrada: String = ""
    private var fechaSalida: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Cargar configuración antes de establecer el content view para evitar ANR
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE))
        setContentView(R.layout.activity_mapa)

        // Obtener datos de la actividad anterior
        adultos = intent.getIntExtra("adultos", 1)
        ninos = intent.getIntExtra("ninos", 0)
        destino = intent.getStringExtra("destino") ?: ""
        fechaEntrada = intent.getStringExtra("fechaEntrada") ?: ""
        fechaSalida = intent.getStringExtra("fechaSalida") ?: ""

        map = findViewById(R.id.mapView)
        map.setMultiTouchControls(true)

        // Encontrar la barra de progreso
        progressBar = findViewById(R.id.mapProgressBar)
        progressBar.visibility = View.VISIBLE

        // Ocultar el mapa inicialmente para evitar problemas de renderizado durante la carga
        map.visibility = View.INVISIBLE

        // Procesar datos del mapa en un hilo en segundo plano usando corrutinas
        lifecycleScope.launch {
            val hoteles = withContext(Dispatchers.IO) {
                // Obtener la lista de hoteles de forma segura
                @Suppress("UNCHECKED_CAST")
                intent.getSerializableExtra("hoteles") as? ArrayList<Hotel> ?: ArrayList()
            }

            // Procesar toda la creación de marcadores en un hilo en segundo plano
            val markers = withContext(Dispatchers.Default) {
                crearMarcadores(hoteles)
            }

            // Actualizar la UI en el hilo principal
            withContext(Dispatchers.Main) {
                // Añadir los marcadores preparados al mapa
                map.overlays.addAll(markers)

                // Hacer zoom para mostrar todos los marcadores si tenemos alguno
                if (markers.isNotEmpty()) {
                    val geoPoints = markers.map { (it as Marker).position }
                    val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                    map.zoomToBoundingBox(boundingBox, true, 100)
                }

                // Mostrar el mapa y ocultar el indicador de carga
                map.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                map.invalidate()
            }
        }
    }

    // Crear marcadores en un hilo en segundo plano
    private fun crearMarcadores(hoteles: List<Hotel>): List<Marker> {
        val markers = mutableListOf<Marker>()

        for (hotel in hoteles) {
            if (hotel.latitud != null && hotel.longitud != null) {
                val point = GeoPoint(hotel.latitud, hotel.longitud)

                val marker = Marker(map)
                marker.position = point

                // Calcular el precio actualizado según el número de personas
                val totalPersonas = adultos + ninos
                val adicionales = (totalPersonas - 2).coerceAtLeast(0)
                val precioFinal = hotel.precioNoche + (adicionales * 22)

                // Mostrar el precio actualizado en el título del marcador
                marker.title = "${precioFinal.toInt()}€"

                // Colocar el nombre del hotel en el subtítulo para mostrarlo al hacer clic
                marker.snippet = hotel.nombre
                marker.subDescription = hotel.ubicacion

                // Configurar evento onClick para el marcador
                marker.setOnMarkerClickListener { clickedMarker, _ ->
                    // Mostrar un Toast con el nombre del hotel
                    Toast.makeText(
                        this@MapaActivity,
                        hotel.nombre,
                        Toast.LENGTH_SHORT
                    ).show()

                    // Mostrar la ventana de información
                    clickedMarker.showInfoWindow()

                    // Al hacer clic en el marcador, navegar a DetalleHotelActivity
                    val intent = Intent(this@MapaActivity, DetalleHotelActivity::class.java)

                    // Incluir todos los datos relevantes
                    intent.putExtra("hotel", hotel)
                    intent.putExtra("adultos", adultos)
                    intent.putExtra("ninos", ninos)
                    intent.putExtra("destino", destino)
                    intent.putExtra("fechaEntrada", fechaEntrada)
                    intent.putExtra("fechaSalida", fechaSalida)
                    intent.putExtra("precioFinal", precioFinal)

                    startActivity(intent)

                    true
                }

                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                markers.add(marker)
            }
        }

        return markers
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}