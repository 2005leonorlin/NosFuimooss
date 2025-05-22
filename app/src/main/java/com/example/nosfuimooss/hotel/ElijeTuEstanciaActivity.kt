package com.example.nosfuimooss.hotel

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Hotel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class ElijeTuEstanciaActivity : AppCompatActivity() {
    private lateinit var tvHotelName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvFechaEntrada: TextView
    private lateinit var tvFechaSalida: TextView
    private lateinit var tvNoches: TextView
    private lateinit var tvPersonas: TextView
    private lateinit var tvPrecioTotalEstancia: TextView // Nuevo TextView para el precio total
    private lateinit var ratingBar: RatingBar
    private lateinit var tvPrecioEstandar: TextView
    private lateinit var tvPrecioSuperior: TextView
    private lateinit var tvPrecioDeluxe: TextView
    private lateinit var btnReservarEstandar: Button
    private lateinit var btnReservarSuperior: Button
    private lateinit var btnReservarDeluxe: Button

    private var hotel: Hotel? = null
    private var adultos: Int = 1
    private var ninos: Int = 0
    private var fechaEntrada: String = ""
    private var fechaSalida: String = ""
    private var precioBase: Double = 0.0
    private var numNoches: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Ocultar ActionBar
        setContentView(R.layout.activity_elije_tu_estancia)

        // Inicializar vistas
        initViews()

        // Obtener datos del Intent
        getIntentData()

        // Calcular número de noches
        calcularNumeroNoches()

        // Actualizar la UI con los datos
        updateUI()

        // Configurar listeners para los botones
        setupButtonListeners()
    }

    private fun initViews() {
        tvHotelName = findViewById(R.id.tvHotelNameEstancia)
        tvLocation = findViewById(R.id.tvLocationEstancia)
        tvFechaEntrada = findViewById(R.id.tvFechaEntradaEstancia)
        tvFechaSalida = findViewById(R.id.tvFechaSalidaEstancia)
        tvNoches = findViewById(R.id.tvNochesEstancia)
        tvPersonas = findViewById(R.id.tvPersonasEstancia)
        tvPrecioTotalEstancia = findViewById(R.id.tvPrecioTotalEstancia) // Inicializar el nuevo TextView
        ratingBar = findViewById(R.id.ratingBarEstancia)
        tvPrecioEstandar = findViewById(R.id.tvPrecioEstandar)
        tvPrecioSuperior = findViewById(R.id.tvPrecioSuperior)
        tvPrecioDeluxe = findViewById(R.id.tvPrecioDeluxe)
        btnReservarEstandar = findViewById(R.id.btnReservarEstandar)
        btnReservarSuperior = findViewById(R.id.btnReservarSuperior)
        btnReservarDeluxe = findViewById(R.id.btnReservarDeluxe)
    }

    private fun getIntentData() {
        hotel = intent.getSerializableExtra("hotel") as? Hotel
        adultos = intent.getIntExtra("adultos", 1)
        ninos = intent.getIntExtra("ninos", 0)
        fechaEntrada = intent.getStringExtra("fechaEntrada") ?: ""
        fechaSalida = intent.getStringExtra("fechaSalida") ?: ""
        precioBase = intent.getDoubleExtra("precioFinal", 0.0)
    }

    private fun calcularNumeroNoches() {
        if (fechaEntrada.isNotEmpty() && fechaSalida.isNotEmpty()) {
            try {
                val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val fechaInicio = formatoFecha.parse(fechaEntrada)
                val fechaFin = formatoFecha.parse(fechaSalida)

                if (fechaInicio != null && fechaFin != null) {
                    val diferencia = fechaFin.time - fechaInicio.time
                    numNoches = TimeUnit.DAYS.convert(diferencia, TimeUnit.MILLISECONDS).toInt()
                    if (numNoches < 1) numNoches = 1
                }
            } catch (e: ParseException) {
                numNoches = 1
            }
        }
    }

    private fun updateUI() {
        hotel?.let { hotel ->
            // Datos del hotel
            tvHotelName.text = hotel.nombre
            tvLocation.text = hotel.ubicacion
            ratingBar.rating = hotel.estrellas.toFloat()

            // Datos de la reserva
            tvFechaEntrada.text = fechaEntrada
            tvFechaSalida.text = fechaSalida
            tvNoches.text = "$numNoches ${if (numNoches == 1) "noche" else "noches"}"
            tvPersonas.text = "$adultos ${if (adultos == 1) "adulto" else "adultos"}, $ninos ${if (ninos == 1) "niño" else "niños"}"

            // Calcular y mostrar el precio total de la estancia (precio base x número de noches)
            val precioTotal = precioBase * numNoches
            tvPrecioTotalEstancia.text = "Precio total: ${precioTotal.toInt()}€"

            // Precios de las habitaciones
            val precioEstandar = precioBase
            val precioSuperior = precioBase * 1.3
            val precioDeluxe = precioBase * 2

            tvPrecioEstandar.text = "${precioEstandar.toInt()}€ / noche"
            tvPrecioSuperior.text = "${precioSuperior.toInt()}€ / noche"
            tvPrecioDeluxe.text = "${precioDeluxe.toInt()}€ / noche"
        }
    }

    private fun setupButtonListeners() {
        btnReservarEstandar.setOnClickListener {
            procesarReserva("Estándar", precioBase)
        }

        btnReservarSuperior.setOnClickListener {
            procesarReserva("Superior", precioBase * 1.3)
        }

        btnReservarDeluxe.setOnClickListener {
            procesarReserva("Deluxe", precioBase * 2)
        }
    }

    private fun procesarReserva(tipoHabitacion: String, precioPorNoche: Double) {
        val precioTotal = precioPorNoche * numNoches


        val intent = Intent(this, InfoHotelActivity::class.java).apply {
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