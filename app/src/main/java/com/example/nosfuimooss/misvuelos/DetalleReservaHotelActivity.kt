package com.example.nosfuimooss.misvuelos

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.usuario.ReservaHotel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalleReservaHotelActivity : AppCompatActivity() {
    private lateinit var reservaHotel: ReservaHotel

    // Vistas
    private lateinit var imgHotel: ImageView
    private lateinit var tvNombreHotel: TextView
    private lateinit var tvUbicacionHotel: TextView
    private lateinit var tvCodigoReserva: TextView
    private lateinit var tvEstadoReserva: TextView
    private lateinit var tvFechaEntrada: TextView
    private lateinit var tvFechaSalida: TextView
    private lateinit var tvTipoHabitacion: TextView
    private lateinit var tvNoches: TextView
    private lateinit var tvHuespedes: TextView
    private lateinit var tvEstrellas: TextView
    private lateinit var tvDireccion: TextView
    private lateinit var tvPrecioTotal: TextView
    private lateinit var tvFechaReserva: TextView
    private lateinit var btnCancelarReserva: Button
    private lateinit var btnVolver: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_reserva_hotel)
        supportActionBar?.hide()

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Obtener datos de la reserva
        reservaHotel = intent.getSerializableExtra("reservaHotel") as ReservaHotel

        // Inicializar vistas
        initViews()

        // Configurar datos
        setupData()

        // Configurar listeners
        setupListeners()
    }

    private fun initViews() {
        imgHotel = findViewById(R.id.img_hotel)
        tvNombreHotel = findViewById(R.id.tv_nombre_hotel)
        tvUbicacionHotel = findViewById(R.id.tv_ubicacion_hotel)
        tvCodigoReserva = findViewById(R.id.tv_codigo_reserva)
        tvEstadoReserva = findViewById(R.id.tv_estado_reserva)
        tvFechaEntrada = findViewById(R.id.tv_fecha_entrada)
        tvFechaSalida = findViewById(R.id.tv_fecha_salida)
        tvTipoHabitacion = findViewById(R.id.tv_tipo_habitacion)
        tvNoches = findViewById(R.id.tv_noches)
        tvHuespedes = findViewById(R.id.tv_huespedes)
        tvEstrellas = findViewById(R.id.tv_estrellas)
        tvDireccion = findViewById(R.id.tv_direccion)
        tvPrecioTotal = findViewById(R.id.tv_precio_total)
        tvFechaReserva = findViewById(R.id.tv_fecha_reserva)
        btnCancelarReserva = findViewById(R.id.btn_cancelar_reserva)
        btnVolver = findViewById(R.id.btn_volver)
    }

    private fun setupData() {
        // Información del hotel
        tvNombreHotel.text = reservaHotel.hotel?.nombre ?: "Hotel desconocido"
        tvUbicacionHotel.text = reservaHotel.hotel?.ubicacion ?: reservaHotel.destino

        // Código y estado de la reserva
        tvCodigoReserva.text = "Código: ${reservaHotel.id}"
        tvEstadoReserva.text = "Estado: ${reservaHotel.estado}"

        // Fechas de estadía
        tvFechaEntrada.text = "Entrada: ${reservaHotel.fechaEntrada}"
        tvFechaSalida.text = "Salida: ${reservaHotel.fechaSalida}"

        // Detalles de la habitación
        tvTipoHabitacion.text = "Habitación: ${reservaHotel.tipoHabitacion}"
        tvNoches.text = "Noches: ${reservaHotel.noches}"

        // Huéspedes
        val totalHuespedes = reservaHotel.adultos + reservaHotel.ninos
        val huespedesText = if (reservaHotel.ninos > 0) {
            "${reservaHotel.adultos} adultos, ${reservaHotel.ninos} niños ($totalHuespedes total)"
        } else {
            "${reservaHotel.adultos} adultos"
        }
        tvHuespedes.text = "Huéspedes: $huespedesText"

        // Estrellas del hotel
        val rating = reservaHotel.hotel?.estrellas ?: 0
        val estrellas = "★".repeat(rating) + "☆".repeat(5 - rating)
        tvEstrellas.text = "Categoría: $estrellas ($rating estrellas)"

        // Dirección
        tvDireccion.text = "Dirección: ${reservaHotel.hotel?.direccion ?: "No disponible"}"

        // Precio total
        tvPrecioTotal.text = "Precio total: ${reservaHotel.precio.toInt()} €"

        // Fecha de la reserva
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaReservaStr = dateFormat.format(Date(reservaHotel.fechaReserva))
        tvFechaReserva.text = "Reservado el: $fechaReservaStr"

        // Cargar imagen del hotel
        val imageUrl = reservaHotel.hotel?.imagenUrl ?: ""
        if (imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.imagen_1)
                .into(imgHotel)
        } else {
            imgHotel.setImageResource(R.drawable.imagen_1)
        }
    }

    private fun setupListeners() {
        btnVolver.setOnClickListener {
            finish()
        }

        btnCancelarReserva.setOnClickListener {
            mostrarDialogoCancelacion()
        }
    }

    private fun mostrarDialogoCancelacion() {
        AlertDialog.Builder(this)
            .setTitle("¿Cancelar reserva?")
            .setMessage("¿Estás seguro de que deseas cancelar esta reserva de hotel? Esta acción no se puede deshacer.")
            .setPositiveButton("Sí, cancelar") { _, _ ->
                cancelarReserva()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelarReserva() {
        val userId = auth.currentUser?.uid ?: return

        // Mostrar diálogo de progreso
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()

        loadingDialog.show()

        // Actualizar el estado en Firebase
        database.reference
            .child("reservasHoteles")
            .child(userId)
            .child(reservaHotel.id)
            .child("estado")
            .setValue("Cancelada")
            .addOnCompleteListener { task ->
                loadingDialog.dismiss()

                if (task.isSuccessful) {
                    Toast.makeText(this, "Reserva cancelada con éxito", Toast.LENGTH_SHORT).show()

                    // Actualizar el estado local y la UI
                    reservaHotel.estado = "Cancelada"
                    tvEstadoReserva.text = "Estado: Cancelada"

                    // Deshabilitar el botón de cancelar
                    btnCancelarReserva.isEnabled = false
                    btnCancelarReserva.text = "Reserva cancelada"

                } else {
                    Toast.makeText(
                        this,
                        "Error al cancelar la reserva: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}