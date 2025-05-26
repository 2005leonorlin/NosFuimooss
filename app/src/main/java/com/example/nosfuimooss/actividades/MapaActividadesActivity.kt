package com.example.nosfuimooss.actividades

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.NosFuimooss.R
import com.example.nosfuimooss.api.RetrofitClient
import com.example.nosfuimooss.model.Actividad
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapaActividadesActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private val activitiesList = ArrayList<Actividad>()
    private var destinoUbicacion: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar OSMDroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        supportActionBar?.hide()
        setContentView(R.layout.activity_mapa_actividades)

        // Obtener el destino desde el intent (si viene filtrado)
        destinoUbicacion = intent.getStringExtra("ubicacionDestino")

        initializeMap()
        loadActivities()
    }

    private fun initializeMap() {
        mapView = findViewById(R.id.map_view)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Configurar zoom inicial y centrar en España
        val mapController = mapView.controller
        mapController.setZoom(6.0)
        val startPoint = GeoPoint(40.4168, -3.7038) // Madrid como punto central
        mapController.setCenter(startPoint)
    }

    private fun loadActivities() {
        val call = RetrofitClient.actividadApiService.getAllActividades()

        call.enqueue(object : Callback<List<Actividad>> {
            override fun onResponse(call: Call<List<Actividad>>, response: Response<List<Actividad>>) {
                if (response.isSuccessful) {
                    val allActivities = response.body() ?: emptyList()

                    // Filtrar actividades por ubicación si se proporcionó un destino
                    val filteredActivities = if (destinoUbicacion != null) {
                        allActivities.filter { actividad ->
                            actividad.ubicacion?.contains(destinoUbicacion!!, ignoreCase = true) == true
                        }
                    } else {
                        allActivities
                    }

                    activitiesList.clear()
                    activitiesList.addAll(filteredActivities)

                    // Agregar marcadores al mapa
                    addMarkersToMap()

                    if (filteredActivities.isNotEmpty()) {
                        // Centrar el mapa en la primera actividad si hay resultados
                        val firstActivity = filteredActivities.first()
                        if (firstActivity.latitud != 0.0 && firstActivity.longitud != 0.0) {
                            val point = GeoPoint(firstActivity.latitud, firstActivity.longitud)
                            mapView.controller.animateTo(point)
                            mapView.controller.setZoom(10.0)
                        }
                    }

                    // Actualizar contador de actividades
                    updateActivitiesCount(filteredActivities.size)

                } else {
                    Toast.makeText(
                        this@MapaActividadesActivity,
                        "Error al cargar actividades",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<Actividad>>, t: Throwable) {
                Toast.makeText(
                    this@MapaActividadesActivity,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun addMarkersToMap() {
        mapView.overlays.clear() // Limpiar marcadores existentes

        for (actividad in activitiesList) {
            // Solo agregar marcador si tiene coordenadas válidas
            if (actividad.latitud != 0.0 && actividad.longitud != 0.0) {
                val marker = Marker(mapView)
                val point = GeoPoint(actividad.latitud, actividad.longitud)

                marker.position = point
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = actividad.nombre ?: "Actividad"
                marker.snippet = buildString {
                    actividad.ubicacion?.let { append(it) }
                    actividad.precio?.let {
                        if (isNotEmpty()) append("\n")
                        append("Precio: $it")
                    }
                }

                // Configurar click en el marcador - manejo directo del click
                marker.setOnMarkerClickListener { clickedMarker, _ ->
                    // Mostrar el InfoWindow
                    clickedMarker.showInfoWindow()

                    // Agregar un Toast o diálogo para indicar que puede tocar de nuevo para ver detalles
                    Toast.makeText(
                        this@MapaActividadesActivity,
                        "Toca de nuevo para ver detalles",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Configurar un segundo click para navegar a detalles
                    setupMarkerDoubleClick(clickedMarker)

                    true
                }

                // Personalizar el ícono del marcador (opcional)
                try {
                    marker.icon = resources.getDrawable(R.drawable.marcador, null)
                } catch (e: Exception) {
                    // Usar ícono por defecto si no existe el drawable
                }

                mapView.overlays.add(marker)
            }
        }

        mapView.invalidate() // Refrescar el mapa
    }

    private fun setupMarkerDoubleClick(marker: Marker) {
        // Crear un listener temporal para el segundo click
        marker.setOnMarkerClickListener { clickedMarker, _ ->
            // Encontrar la actividad correspondiente y navegar a detalles
            val actividadSeleccionada = activitiesList.find {
                it.latitud == clickedMarker.position.latitude &&
                        it.longitud == clickedMarker.position.longitude &&
                        it.nombre == clickedMarker.title
            }

            actividadSeleccionada?.let { actividad ->
                val intent = Intent(this@MapaActividadesActivity, DetalleActividadActivity::class.java)
                intent.putExtra("actividad", actividad)
                startActivity(intent)
            }

            true
        }
    }

    private fun updateActivitiesCount(count: Int) {
        try {
            val countTextView = findViewById<android.widget.TextView>(R.id.activities_count)
            val text = if (count == 1) {
                "$count actividad encontrada"
            } else {
                "$count actividades encontradas"
            }
            countTextView.text = text
        } catch (e: Exception) {
            // Si no existe el TextView, ignorar
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }
}