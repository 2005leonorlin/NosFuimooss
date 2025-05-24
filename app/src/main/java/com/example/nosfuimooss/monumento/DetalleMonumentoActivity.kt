package com.example.nosfuimooss.monumento

import android.os.Bundle
import android.preference.PreferenceManager
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.api.RetrofitClient
import com.example.nosfuimooss.model.Monumento
import org.osmdroid.config.Configuration
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class DetalleMonumentoActivity : AppCompatActivity() {
    private lateinit var monumentoImage: ImageView
    private lateinit var monumentoNombre: TextView
    private lateinit var monumentoInformacion: TextView
    private lateinit var monumentoUbicacion: TextView
    private lateinit var monumentoDireccion: TextView
    private lateinit var ratingText: TextView
    private lateinit var mapWebView: WebView
    private lateinit var btnGuardar: Button

    // Estrellas
    private lateinit var starImages: List<ImageView>

    // Cards de monumentos cercanos
    private lateinit var cardMonumento1: CardView
    private lateinit var cardMonumento2: CardView
    private lateinit var imgMonumentoCercano1: ImageView
    private lateinit var imgMonumentoCercano2: ImageView
    private lateinit var txtNombreMonumento1: TextView
    private lateinit var txtNombreMonumento2: TextView
    private lateinit var txtDistanciaMonumento1: TextView
    private lateinit var txtDistanciaMonumento2: TextView
    private lateinit var txtRatingMonumento1: TextView
    private lateinit var txtRatingMonumento2: TextView

    private var monumentoActual: Monumento? = null
    private var monumentosCercanos: List<Monumento> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar OSMDroid antes de setContentView
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        setContentView(R.layout.activity_detalle_monumento)
        supportActionBar?.hide()

        // Obtener el monumento del intent
        monumentoActual = intent.getSerializableExtra("monumento") as? Monumento

        if (monumentoActual == null) {
            Toast.makeText(this, "Error: No se pudo cargar el monumento", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupMonumentoData()
        setupMapWebView()
        loadMonumentosCercanos()
        setupClickListeners()
    }

    private fun initializeViews() {
        monumentoImage = findViewById(R.id.monumento_image)
        monumentoNombre = findViewById(R.id.monumento_nombre)
        monumentoInformacion = findViewById(R.id.monumento_informacion)
        monumentoUbicacion = findViewById(R.id.monumento_ubicacion)
        monumentoDireccion = findViewById(R.id.monumento_direccion)
        ratingText = findViewById(R.id.rating_text)
        mapWebView = findViewById(R.id.map_webview)
        btnGuardar = findViewById(R.id.btn_guardar)

        // Inicializar estrellas
        starImages = listOf(
            findViewById(R.id.star_1),
            findViewById(R.id.star_2),
            findViewById(R.id.star_3),
            findViewById(R.id.star_4),
            findViewById(R.id.star_5)
        )

        // Inicializar cards de monumentos cercanos
        cardMonumento1 = findViewById(R.id.card_monumento_cercano_1)
        cardMonumento2 = findViewById(R.id.card_monumento_cercano_2)
        imgMonumentoCercano1 = findViewById(R.id.img_monumento_cercano_1)
        imgMonumentoCercano2 = findViewById(R.id.img_monumento_cercano_2)
        txtNombreMonumento1 = findViewById(R.id.txt_nombre_monumento_1)
        txtNombreMonumento2 = findViewById(R.id.txt_nombre_monumento_2)
        txtDistanciaMonumento1 = findViewById(R.id.txt_distancia_monumento_1)
        txtDistanciaMonumento2 = findViewById(R.id.txt_distancia_monumento_2)
        txtRatingMonumento1 = findViewById(R.id.txt_rating_monumento_1)
        txtRatingMonumento2 = findViewById(R.id.txt_rating_monumento_2)
    }

    private fun setupMonumentoData() {
        monumentoActual?.let { monumento ->
            // Cargar imagen principal
            Glide.with(this)
                .load(monumento.foto)
                .placeholder(R.drawable.imagen_1)
                .error(R.drawable.imagen_1)
                .centerCrop()
                .into(monumentoImage)

            // Configurar textos
            monumentoNombre.text = monumento.nombre?.uppercase() ?: "MONUMENTO SIN NOMBRE"
            monumentoInformacion.text = monumento.informacion ?: "Sin información disponible sobre este monumento."
            monumentoUbicacion.text = monumento.ubicacion ?: "Ubicación no disponible"
            monumentoDireccion.text = monumento.direccion ?: "Dirección no disponible"

            // Configurar rating
            val rating = monumento.estrella ?: 0.0
            ratingText.text = rating.toString()
            setupStars(rating)
        }
    }

    private fun setupStars(rating: Double) {
        val fullStars = rating.toInt()
        val hasHalfStar = rating - fullStars >= 0.5

        for (i in starImages.indices) {
            when {
                i < fullStars -> {
                    starImages[i].setImageResource(R.drawable.ic_star)
                    starImages[i].imageTintList = getColorStateList(android.R.color.holo_orange_light)
                }
                i == fullStars && hasHalfStar -> {
                    starImages[i].setImageResource(R.drawable.ic_star)
                    starImages[i].imageTintList = getColorStateList(android.R.color.holo_orange_light)
                }
                else -> {
                    starImages[i].setImageResource(R.drawable.ic_star)
                    starImages[i].imageTintList = getColorStateList(android.R.color.darker_gray)
                }
            }
        }
    }

    private fun setupMapWebView() {
        monumentoActual?.let { monumento ->
            val lat = monumento.latitud ?: 40.4168
            val lon = monumento.longitud ?: -3.7038

            mapWebView.settings.javaScriptEnabled = true
            mapWebView.settings.domStorageEnabled = true

            val mapHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
                    <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
                    <style>
                        body { margin: 0; padding: 0; }
                        #map { height: 100vh; width: 100%; }
                    </style>
                </head>
                <body>
                    <div id="map"></div>
                    <script>
                        var map = L.map('map').setView([$lat, $lon], 15);
                        
                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            attribution: '© OpenStreetMap contributors'
                        }).addTo(map);
                        
                        var marker = L.marker([$lat, $lon]).addTo(map)
                            .bindPopup('${monumento.nombre ?: "Monumento"}')
                            .openPopup();
                    </script>
                </body>
                </html>
            """.trimIndent()

            mapWebView.loadDataWithBaseURL(null, mapHtml, "text/html", "utf-8", null)
        }
    }

    private fun loadMonumentosCercanos() {
        monumentoActual?.ubicacion?.let { ubicacion ->
            val call = RetrofitClient.monumentoApiService.getMonumentosByUbicacion(ubicacion.trim())

            call.enqueue(object : Callback<List<Monumento>> {
                override fun onResponse(call: Call<List<Monumento>>, response: Response<List<Monumento>>) {
                    if (response.isSuccessful) {
                        val monumentos = response.body()
                        if (monumentos != null) {
                            // Filtrar el monumento actual, calcular distancias y ordenar por distancia
                            monumentosCercanos = monumentos
                                .filter { it.id != monumentoActual?.id }
                                .filter { it.latitud != null && it.longitud != null }
                                .sortedBy { monumento ->
                                    calculateDistance(
                                        monumentoActual?.latitud ?: 0.0,
                                        monumentoActual?.longitud ?: 0.0,
                                        monumento.latitud ?: 0.0,
                                        monumento.longitud ?: 0.0
                                    )
                                }
                                .take(2)

                            setupMonumentosCercanos()
                        }
                    }
                }

                override fun onFailure(call: Call<List<Monumento>>, t: Throwable) {
                    // Silenciar el error ya que es opcional
                }
            })
        }
    }

    private fun setupMonumentosCercanos() {
        if (monumentosCercanos.isNotEmpty()) {
            // Primer monumento cercano
            val monumento1 = monumentosCercanos[0]

            Glide.with(this)
                .load(monumento1.foto)
                .placeholder(R.drawable.imagen_1)
                .error(R.drawable.imagen_1)
                .centerCrop()
                .into(imgMonumentoCercano1)

            txtNombreMonumento1.text = monumento1.nombre ?: "Monumento"

            // Calcular y mostrar distancia real
            val distancia1 = calculateDistance(
                monumentoActual?.latitud ?: 0.0,
                monumentoActual?.longitud ?: 0.0,
                monumento1.latitud ?: 0.0,
                monumento1.longitud ?: 0.0
            )
            txtDistanciaMonumento1.text = formatDistance(distancia1)
            txtRatingMonumento1.text = (monumento1.estrella ?: 0.0).toString()

            // Segundo monumento cercano si existe
            if (monumentosCercanos.size > 1) {
                val monumento2 = monumentosCercanos[1]

                Glide.with(this)
                    .load(monumento2.foto)
                    .placeholder(R.drawable.imagen_2)
                    .error(R.drawable.imagen_2)
                    .centerCrop()
                    .into(imgMonumentoCercano2)

                txtNombreMonumento2.text = monumento2.nombre ?: "Monumento"

                // Calcular y mostrar distancia real
                val distancia2 = calculateDistance(
                    monumentoActual?.latitud ?: 0.0,
                    monumentoActual?.longitud ?: 0.0,
                    monumento2.latitud ?: 0.0,
                    monumento2.longitud ?: 0.0
                )
                txtDistanciaMonumento2.text = formatDistance(distancia2)
                txtRatingMonumento2.text = (monumento2.estrella ?: 0.0).toString()
            }
        }
    }

    /**
     * Calcula la distancia entre dos puntos usando la fórmula de Haversine
     * @param lat1 Latitud del primer punto
     * @param lon1 Longitud del primer punto
     * @param lat2 Latitud del segundo punto
     * @param lon2 Longitud del segundo punto
     * @return Distancia en kilómetros
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Radio de la Tierra en kilómetros

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Formatea la distancia para mostrar en la UI
     * @param distanceKm Distancia en kilómetros
     * @return String formateado con la distancia
     */
    private fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 1.0 -> {
                val distanceM = (distanceKm * 1000).roundToInt()
                "${distanceM}m"
            }
            distanceKm < 10.0 -> {
                String.format("%.1f km", distanceKm)
            }
            else -> {
                "${distanceKm.roundToInt()} km"
            }
        }
    }

    private fun setupClickListeners() {
        btnGuardar.setOnClickListener {
            // Navegar a GuardarMonumentoActivity
            val intent = android.content.Intent(this, GuardarMonumentoActivity::class.java)
            intent.putExtra("monumento", monumentoActual)
            startActivity(intent)
        }

        // Click en monumentos cercanos
        cardMonumento1.setOnClickListener {
            if (monumentosCercanos.isNotEmpty()) {
                navigateToMonumentoDetail(monumentosCercanos[0])
            }
        }

        cardMonumento2.setOnClickListener {
            if (monumentosCercanos.size > 1) {
                navigateToMonumentoDetail(monumentosCercanos[1])
            }
        }
    }

    private fun navigateToMonumentoDetail(monumento: Monumento) {
        val intent = android.content.Intent(this, DetalleMonumentoActivity::class.java)
        intent.putExtra("monumento", monumento)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}