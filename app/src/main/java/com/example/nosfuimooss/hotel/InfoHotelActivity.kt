package com.example.nosfuimooss.hotel

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Hotel
import java.text.SimpleDateFormat
import java.util.Locale

class InfoHotelActivity : AppCompatActivity() {
    private lateinit var tvHotelName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvTipoHabitacion: TextView
    private lateinit var tvFechaEntrada: TextView
    private lateinit var tvFechaSalida: TextView
    private lateinit var tvNoches: TextView
    private lateinit var tvPersonas: TextView
    private lateinit var tvPrecioPorNoche: TextView
    private lateinit var tvPrecioTotal: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var ivLocationImage: ImageView
    private lateinit var btnCancelar: Button
    private lateinit var btnContinuar: Button

    private var hotel: Hotel? = null
    private var tipoHabitacion: String = ""
    private var precioPorNoche: Double = 0.0
    private var precioTotal: Double = 0.0
    private var adultos: Int = 1
    private var ninos: Int = 0
    private var fechaEntrada: String = ""
    private var fechaSalida: String = ""
    private var numNoches: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_info_hotel)

        // Inicializar vistas
        initViews()

        // Obtener datos del Intent
        getIntentData()

        // Actualizar la UI con los datos
        updateUI()

        // Configurar listeners para los botones
        setupButtonListeners()
    }

    private fun initViews() {
        tvHotelName = findViewById(R.id.tvHotelNameInfo)
        tvLocation = findViewById(R.id.tvLocationInfo)
        tvTipoHabitacion = findViewById(R.id.tvTipoHabitacionInfo)
        tvFechaEntrada = findViewById(R.id.tvFechaEntradaInfo)
        tvFechaSalida = findViewById(R.id.tvFechaSalidaInfo)
        tvNoches = findViewById(R.id.tvNochesInfo)
        tvPersonas = findViewById(R.id.tvPersonasInfo)
        tvPrecioPorNoche = findViewById(R.id.tvPrecioPorNocheInfo)
        tvPrecioTotal = findViewById(R.id.tvPrecioTotalInfo)
        ratingBar = findViewById(R.id.ratingBarInfo)
        ivLocationImage = findViewById(R.id.ivLocationImage)
        btnCancelar = findViewById(R.id.btnCancelarInfo)
        btnContinuar = findViewById(R.id.btnContinuarInfo)
    }

    private fun getIntentData() {
        hotel = intent.getSerializableExtra("hotel") as? Hotel
        tipoHabitacion = intent.getStringExtra("tipoHabitacion") ?: ""
        precioPorNoche = intent.getDoubleExtra("precioPorNoche", 0.0)
        precioTotal = intent.getDoubleExtra("precioTotal", 0.0)
        adultos = intent.getIntExtra("adultos", 1)
        ninos = intent.getIntExtra("ninos", 0)
        fechaEntrada = intent.getStringExtra("fechaEntrada") ?: ""
        fechaSalida = intent.getStringExtra("fechaSalida") ?: ""
        numNoches = intent.getIntExtra("numNoches", 1)
    }

    private fun updateUI() {
        hotel?.let { hotel ->
            // Datos del hotel
            tvHotelName.text = hotel.nombre
            tvLocation.text = hotel.ubicacion
            ratingBar.rating = hotel.estrellas.toFloat()

            // Cargar imagen del hotel
            cargarImagenHotel(hotel)

            // Formatear las fechas al estilo español (dd/MM/yyyy)
            val formatoFechaOriginal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatoFechaEspanol = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            try {
                val fechaEntradaDate = formatoFechaOriginal.parse(fechaEntrada)
                val fechaSalidaDate = formatoFechaOriginal.parse(fechaSalida)

                if (fechaEntradaDate != null && fechaSalidaDate != null) {
                    val fechaEntradaFormateada = formatoFechaEspanol.format(fechaEntradaDate)
                    val fechaSalidaFormateada = formatoFechaEspanol.format(fechaSalidaDate)

                    tvFechaEntrada.text = "Entrada: $fechaEntradaFormateada"
                    tvFechaSalida.text = "Salida: $fechaSalidaFormateada"
                } else {
                    tvFechaEntrada.text = "Entrada: $fechaEntrada"
                    tvFechaSalida.text = "Salida: $fechaSalida"
                }
            } catch (e: Exception) {
                tvFechaEntrada.text = "Entrada: $fechaEntrada"
                tvFechaSalida.text = "Salida: $fechaSalida"
            }

            // Información de la estancia
            tvTipoHabitacion.text = "Habitación $tipoHabitacion"
            tvNoches.text = "$numNoches ${if (numNoches == 1) "noche" else "noches"}"
            tvPersonas.text = "$adultos ${if (adultos == 1) "adulto" else "adultos"}, $ninos ${if (ninos == 1) "niño" else "niños"}"

            // Información de precios
            tvPrecioPorNoche.text = "${precioPorNoche.toInt()}€ / noche"
            tvPrecioTotal.text = "Total: ${precioTotal.toInt()}€"
        }
    }

    private fun cargarImagenHotel(hotel: Hotel) {
        // Primero intentamos cargar de la lista de imágenes, si existe y no está vacía
        val imagenUrl = if (!hotel.imagenes.isNullOrEmpty()) {
            // Tomamos la primera imagen de la lista
            hotel.imagenes[0]
        } else {
            // Si no hay lista de imágenes, usamos la imagen principal
            hotel.imagenUrl
        }

        try {
            // Cargar la imagen con Glide
            Glide.with(this)
                .load(imagenUrl)
                .placeholder(R.drawable.imagen_1) // Asegúrate de tener esta imagen
                .error(R.drawable.imagen_1) // Asegúrate de tener esta imagen
                .into(ivLocationImage)
        } catch (e: Exception) {
            // Si falla la carga, establecer una imagen por defecto
            ivLocationImage.setImageResource(R.drawable.imagen_1) // Asegúrate de tener esta imagen
        }
    }

    private fun setupButtonListeners() {
        btnCancelar.setOnClickListener {
            // Volver a la pantalla de buscar hoteles
            val intent = Intent(this, ReservarHotel::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        btnContinuar.setOnClickListener {
            // Navegar a la pantalla de datos personales
            val intent = Intent(this, DatosPersonasActivity::class.java).apply {
                putExtra("hotel", hotel)
                putExtra("tipoHabitacion", tipoHabitacion)
                putExtra("precioPorNoche", precioPorNoche)
                putExtra("precioTotal", precioTotal)
                putExtra("adultos", adultos)
                putExtra("ninos", ninos)
                putExtra("fechaEntrada", fechaEntrada)
                putExtra("fechaSalida", fechaSalida)
                putExtra("numNoches", numNoches)
            }
            startActivity(intent)
        }
    }
}