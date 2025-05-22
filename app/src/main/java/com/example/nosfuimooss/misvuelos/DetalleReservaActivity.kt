package com.example.nosfuimooss.misvuelos

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.usuario.Reserva
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalleReservaActivity : AppCompatActivity() {

    private lateinit var reserva: Reserva

    // Vistas principales
    private lateinit var imgDestino: ImageView
    private lateinit var tvCodigoReserva: TextView
    private lateinit var tvEstadoReserva: TextView
    private lateinit var tvRutaCompleta: TextView
    private lateinit var tvPrecioTotal: TextView
    private lateinit var tvFechaReserva: TextView

    // Información del viaje
    private lateinit var tvTipoViaje: TextView
    private lateinit var tvTarifa: TextView
    private lateinit var tvClase: TextView

    // Sección de vuelo de ida
    private lateinit var cardVueloIda: CardView
    private lateinit var tvIdaFecha: TextView
    private lateinit var tvIdaRuta: TextView
    private lateinit var tvIdaHoraSalida: TextView
    private lateinit var tvIdaHoraLlegada: TextView
    private lateinit var tvIdaDuracion: TextView

    // Sección de vuelo de vuelta
    private lateinit var cardVueloVuelta: CardView
    private lateinit var tvVueltaFecha: TextView
    private lateinit var tvVueltaRuta: TextView
    private lateinit var tvVueltaHoraSalida: TextView
    private lateinit var tvVueltaHoraLlegada: TextView
    private lateinit var tvVueltaDuracion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_reserva)
        supportActionBar?.hide()

        // Obtener la reserva del intent
        reserva = intent.getSerializableExtra("reserva") as? Reserva
            ?: run {
                Toast.makeText(this, "Error al cargar los detalles de la reserva", Toast.LENGTH_LONG).show()
                finish()
                return
            }

        initViews()
        setupUI()

        Log.d("DetalleReserva", "Mostrando detalles para reserva: ${reserva.id}")
    }

    private fun initViews() {
        // Vistas principales
        imgDestino = findViewById(R.id.img_destino_detalle)
        tvCodigoReserva = findViewById(R.id.tv_codigo_reserva)
        tvEstadoReserva = findViewById(R.id.tv_estado_reserva)
        tvRutaCompleta = findViewById(R.id.tv_ruta_completa)
        tvPrecioTotal = findViewById(R.id.tv_precio_total)
        tvFechaReserva = findViewById(R.id.tv_fecha_reserva)

        // Información del viaje
        tvTipoViaje = findViewById(R.id.tv_tipo_viaje)
        tvTarifa = findViewById(R.id.tv_tarifa)
        tvClase = findViewById(R.id.tv_clase)

        // Vuelo de ida
        cardVueloIda = findViewById(R.id.card_vuelo_ida)
        tvIdaFecha = findViewById(R.id.tv_ida_fecha)
        tvIdaRuta = findViewById(R.id.tv_ida_ruta)
        tvIdaHoraSalida = findViewById(R.id.tv_ida_hora_salida)
        tvIdaHoraLlegada = findViewById(R.id.tv_ida_hora_llegada)
        tvIdaDuracion = findViewById(R.id.tv_ida_duracion)

        // Vuelo de vuelta
        cardVueloVuelta = findViewById(R.id.card_vuelo_vuelta)
        tvVueltaFecha = findViewById(R.id.tv_vuelta_fecha)
        tvVueltaRuta = findViewById(R.id.tv_vuelta_ruta)
        tvVueltaHoraSalida = findViewById(R.id.tv_vuelta_hora_salida)
        tvVueltaHoraLlegada = findViewById(R.id.tv_vuelta_hora_llegada)
        tvVueltaDuracion = findViewById(R.id.tv_vuelta_duracion)

        // Botón de volver
        findViewById<View>(R.id.btn_volver).setOnClickListener {
            finish()
        }
    }

    private fun setupUI() {
        setupHeaderInfo()
        setupFlightInfo()
        setupTravelDetails()
    }

    private fun setupHeaderInfo() {
        // Código de reserva
        tvCodigoReserva.text = "Código: ${reserva.id.take(8).uppercase()}"

        // Estado de la reserva
        tvEstadoReserva.text = reserva.estado

        // Aplicar el fondo según el estado
        when (reserva.estado) {
            "Confirmada" -> {
                tvEstadoReserva.setTextColor(getColor(android.R.color.holo_green_dark))
                tvEstadoReserva.setBackgroundResource(R.drawable.bg_estado_reserva)
            }
            "Pendiente" -> {
                tvEstadoReserva.setTextColor(getColor(android.R.color.holo_orange_dark))
                tvEstadoReserva.setBackgroundResource(R.drawable.bg_estado_reserva)
            }
            "Cancelada" -> {
                tvEstadoReserva.setTextColor(getColor(android.R.color.holo_red_dark))
                tvEstadoReserva.setBackgroundResource(R.drawable.bg_estado_reserva)
            }
            else -> {
                tvEstadoReserva.setTextColor(getColor(android.R.color.darker_gray))
                tvEstadoReserva.setBackgroundResource(R.drawable.bg_estado_reserva)
            }
        }

        // Ruta completa
        val origen = reserva.boletoIda?.origen ?: "Origen desconocido"
        val destino = reserva.boletoIda?.destino ?: "Destino desconocido"
        tvRutaCompleta.text = if (reserva.roundTrip) {
            "$origen ↔ $destino"
        } else {
            "$origen → $destino"
        }

        // Precio total
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        tvPrecioTotal.text = numberFormat.format(reserva.precio)

        // Fecha de reserva
        val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy 'a las' HH:mm", Locale("es", "ES"))
        tvFechaReserva.text = "Reservado el ${dateFormat.format(Date(reserva.fechaReserva))}"

        // Imagen del destino
        if (reserva.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(reserva.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.imagen_1)
                .error(R.drawable.imagen_1)
                .into(imgDestino)
        } else {
            imgDestino.setImageResource(R.drawable.imagen_1)
        }
    }

    private fun setupTravelDetails() {
        // Tipo de viaje
        tvTipoViaje.text = if (reserva.roundTrip) "Ida y vuelta" else "Solo ida"

        // Tarifa
        tvTarifa.text = if (reserva.tarifa.isNotEmpty()) reserva.tarifa else "No especificada"

        // Clase
        tvClase.text = if (reserva.clase.isNotEmpty()) reserva.clase else "No especificada"
    }

    private fun setupFlightInfo() {
        setupFlightIda()
        setupFlightVuelta()
    }

    private fun setupFlightIda() {
        val dateFormat = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))

        // Fecha del vuelo de ida
        if (reserva.fechaIda > 0) {
            tvIdaFecha.text = dateFormat.format(Date(reserva.fechaIda))
        } else {
            tvIdaFecha.text = "Fecha no disponible"
        }

        // Ruta de ida
        val origen = reserva.boletoIda?.origen ?: "Origen desconocido"
        val destino = reserva.boletoIda?.destino ?: "Destino desconocido"
        tvIdaRuta.text = "$origen → $destino"

        // Horarios de ida
        tvIdaHoraSalida.text = if (reserva.idaDepartureTime.isNotEmpty()) {
            reserva.idaDepartureTime
        } else {
            "No disponible"
        }

        tvIdaHoraLlegada.text = if (reserva.idaArrivalTime.isNotEmpty()) {
            reserva.idaArrivalTime
        } else {
            "No disponible"
        }

        // Duración estimada (si tenemos ambos horarios)
        if (reserva.idaDepartureTime.isNotEmpty() && reserva.idaArrivalTime.isNotEmpty()) {
            try {
                val duracion = calcularDuracionVuelo(reserva.idaDepartureTime, reserva.idaArrivalTime)
                tvIdaDuracion.text = duracion
            } catch (e: Exception) {
                tvIdaDuracion.text = "Duración no disponible"
            }
        } else {
            tvIdaDuracion.text = "Duración no disponible"
        }
    }

    private fun setupFlightVuelta() {
        if (reserva.roundTrip) {
            cardVueloVuelta.visibility = View.VISIBLE

            val dateFormat = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))

            // Fecha del vuelo de vuelta
            if (reserva.fechaVuelta > 0) {
                tvVueltaFecha.text = dateFormat.format(Date(reserva.fechaVuelta))
            } else {
                tvVueltaFecha.text = "Fecha no disponible"
            }

            // Ruta de vuelta
            val origenVuelta = if (reserva.boletoVuelta != null && reserva.boletoVuelta!!.origen.isNotEmpty()) {
                reserva.boletoVuelta!!.origen
            } else {
                reserva.boletoIda?.destino ?: "Origen desconocido"
            }

            val destinoVuelta = if (reserva.boletoVuelta != null && reserva.boletoVuelta!!.destino.isNotEmpty()) {
                reserva.boletoVuelta!!.destino
            } else {
                reserva.boletoIda?.origen ?: "Destino desconocido"
            }

            tvVueltaRuta.text = "$origenVuelta → $destinoVuelta"

            // Horarios de vuelta
            tvVueltaHoraSalida.text = if (reserva.vueltaDepartureTime.isNotEmpty()) {
                reserva.vueltaDepartureTime
            } else {
                "No disponible"
            }

            tvVueltaHoraLlegada.text = if (reserva.vueltaArrivalTime.isNotEmpty()) {
                reserva.vueltaArrivalTime
            } else {
                "No disponible"
            }

            // Duración estimada del vuelo de vuelta
            if (reserva.vueltaDepartureTime.isNotEmpty() && reserva.vueltaArrivalTime.isNotEmpty()) {
                try {
                    val duracion = calcularDuracionVuelo(reserva.vueltaDepartureTime, reserva.vueltaArrivalTime)
                    tvVueltaDuracion.text = duracion
                } catch (e: Exception) {
                    tvVueltaDuracion.text = "Duración no disponible"
                }
            } else {
                tvVueltaDuracion.text = "Duración no disponible"
            }
        } else {
            cardVueloVuelta.visibility = View.GONE
        }
    }

    private fun calcularDuracionVuelo(horaSalida: String, horaLlegada: String): String {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val salida = timeFormat.parse(horaSalida)
            val llegada = timeFormat.parse(horaLlegada)

            if (salida != null && llegada != null) {
                val duracionMs = llegada.time - salida.time

                // Si la duración es negativa, significa que el vuelo llega al día siguiente
                val ajustedDuration = if (duracionMs < 0) {
                    duracionMs + 24 * 60 * 60 * 1000 // Agregar 24 horas
                } else {
                    duracionMs
                }

                val duracionHoras = ajustedDuration / (1000 * 60 * 60)
                val duracionMinutos = (ajustedDuration % (1000 * 60 * 60)) / (1000 * 60)

                return when {
                    duracionHoras > 0 -> "${duracionHoras}h ${duracionMinutos}min"
                    duracionMinutos > 0 -> "${duracionMinutos}min"
                    else -> "Duración no disponible"
                }
            }
        } catch (e: Exception) {
            Log.e("DetalleReserva", "Error calculando duración: ${e.message}")
        }
        return "Duración no disponible"
    }
}