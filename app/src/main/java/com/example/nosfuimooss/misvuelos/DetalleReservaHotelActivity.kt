package com.example.nosfuimooss.misvuelos

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.Adapter.MonumentosGuardadosAdapter
import com.example.nosfuimooss.model.Monumento
import com.example.nosfuimooss.usuario.ReservaHotel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalleReservaHotelActivity : AppCompatActivity() {
    private lateinit var reservaHotel: ReservaHotel

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

    // Nuevas vistas para monumentos
    private lateinit var recyclerMonumentos: RecyclerView
    private lateinit var tvNoMonumentos: TextView
    private lateinit var monumentosAdapter: MonumentosGuardadosAdapter

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Lista de monumentos guardados
    private val monumentosGuardados: MutableList<Monumento> = mutableListOf()

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

        // Configurar RecyclerView de monumentos
        setupMonumentosRecyclerView()

        // Cargar monumentos guardados
        loadMonumentosGuardados()

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

        // Nuevas vistas para monumentos
        recyclerMonumentos = findViewById(R.id.recycler_monumentos)
        tvNoMonumentos = findViewById(R.id.tv_no_monumentos)
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

    private fun setupMonumentosRecyclerView() {
        monumentosAdapter = MonumentosGuardadosAdapter(monumentosGuardados) { monumento ->
            // Aquí puedes agregar la funcionalidad para ver detalles del monumento
            // o eliminarlo de la lista si lo deseas
            mostrarDetalleMonumento(monumento)
        }

        recyclerMonumentos.apply {
            layoutManager = LinearLayoutManager(this@DetalleReservaHotelActivity)
            adapter = monumentosAdapter
        }
    }

    private fun loadMonumentosGuardados() {
        val userId = auth.currentUser?.uid ?: return

        println("=== CARGANDO MONUMENTOS GUARDADOS ===")
        println("Usuario ID: $userId")
        println("Reserva ID: ${reservaHotel.id}")

        database.reference
            .child("reservasHoteles")
            .child(userId)
            .child(reservaHotel.id)
            .child("monumentosGuardados")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    println("Snapshot existe: ${snapshot.exists()}")
                    println("Número de monumentos: ${snapshot.childrenCount}")

                    monumentosGuardados.clear()

                    if (snapshot.exists()) {
                        for (monumentoSnapshot in snapshot.children) {
                            try {
                                val monumento = monumentoSnapshot.getValue(Monumento::class.java)
                                monumento?.let {
                                    monumentosGuardados.add(it)
                                    println("Monumento cargado: ${it.nombre}")
                                }
                            } catch (e: Exception) {
                                println("Error al cargar monumento: ${e.message}")
                            }
                        }
                    }

                    updateMonumentosUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Error al cargar monumentos: ${error.message}")
                    Toast.makeText(
                        this@DetalleReservaHotelActivity,
                        "Error al cargar monumentos guardados",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateMonumentosUI() {
        if (monumentosGuardados.isEmpty()) {
            recyclerMonumentos.visibility = View.GONE
            tvNoMonumentos.visibility = View.VISIBLE
            tvNoMonumentos.text = "No tienes monumentos guardados para esta reserva.\n\nPuedes guardar monumentos desde la sección de explorar lugares."
        } else {
            recyclerMonumentos.visibility = View.VISIBLE
            tvNoMonumentos.visibility = View.GONE
            monumentosAdapter.notifyDataSetChanged()
        }

        println("UI actualizada - Monumentos: ${monumentosGuardados.size}")
    }

    private fun mostrarDetalleMonumento(monumento: Monumento) {
        AlertDialog.Builder(this)
            .setTitle(monumento.nombre)
            .setMessage("${monumento.ubicacion}\n\n¿Qué deseas hacer con este monumento?")
            .setPositiveButton("Ver detalles") { _, _ ->
                // Aquí podrías navegar a una actividad de detalle del monumento
                Toast.makeText(this, "Función de detalles próximamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Eliminar") { _, _ ->
                confirmarEliminarMonumento(monumento)
            }
            .setNeutralButton("Cerrar", null)
            .show()
    }

    private fun confirmarEliminarMonumento(monumento: Monumento) {
        AlertDialog.Builder(this)
            .setTitle("¿Eliminar monumento?")
            .setMessage("¿Estás seguro de que deseas eliminar '${monumento.nombre}' de esta reserva?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarMonumento(monumento)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarMonumento(monumento: Monumento) {
        val userId = auth.currentUser?.uid ?: return

        // Mostrar diálogo de progreso
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Obtener la lista actual y eliminar el monumento
        database.reference
            .child("reservasHoteles")
            .child(userId)
            .child(reservaHotel.id)
            .child("monumentosGuardados")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val monumentosActualizados = mutableListOf<Monumento>()

                    // Obtener todos los monumentos excepto el que queremos eliminar
                    for (monumentoSnapshot in snapshot.children) {
                        val monumentoExistente = monumentoSnapshot.getValue(Monumento::class.java)
                        if (monumentoExistente != null && monumentoExistente.id != monumento.id) {
                            monumentosActualizados.add(monumentoExistente)
                        }
                    }

                    // Guardar la lista actualizada
                    database.reference
                        .child("reservasHoteles")
                        .child(userId)
                        .child(reservaHotel.id)
                        .child("monumentosGuardados")
                        .setValue(monumentosActualizados)
                        .addOnCompleteListener { task ->
                            loadingDialog.dismiss()

                            if (task.isSuccessful) {
                                Toast.makeText(
                                    this@DetalleReservaHotelActivity,
                                    "Monumento eliminado exitosamente",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@DetalleReservaHotelActivity,
                                    "Error al eliminar monumento: ${task.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@DetalleReservaHotelActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
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