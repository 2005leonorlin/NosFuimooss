package com.example.nosfuimooss.monumento

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.Adapter.boleto.ReservasSeleccionAdapter
import com.example.nosfuimooss.model.Monumento
import com.example.nosfuimooss.usuario.ReservaHotel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GuardarMonumentoActivity : AppCompatActivity() {
    private lateinit var imgMonumento: ImageView
    private lateinit var tvNombreMonumento: TextView
    private lateinit var tvUbicacionMonumento: TextView
    private lateinit var tvInfoMonumento: TextView

    // RecyclerView y adaptador
    private lateinit var recyclerReservas: RecyclerView
    private lateinit var tvNoReservas: TextView
    private lateinit var btnVolver: Button
    private lateinit var reservasAdapter: ReservasSeleccionAdapter

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Datos
    private var monumentoActual: Monumento? = null
    private var reservasDisponibles: MutableList<ReservaHotel> = mutableListOf()
    private var reservaSeleccionada: ReservaHotel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guardar_monumento)
        supportActionBar?.hide()

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Obtener el monumento del intent
        monumentoActual = intent.getSerializableExtra("monumento") as? Monumento

        if (monumentoActual == null) {
            Toast.makeText(this, "Error: No se pudo cargar el monumento", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupMonumentoData()
        setupRecyclerView()
        loadReservasHoteles()
        setupClickListeners()
    }

    private fun initializeViews() {
        imgMonumento = findViewById(R.id.img_monumento)
        tvNombreMonumento = findViewById(R.id.tv_nombre_monumento)
        tvUbicacionMonumento = findViewById(R.id.tv_ubicacion_monumento)
        tvInfoMonumento = findViewById(R.id.tv_info_monumento)
        recyclerReservas = findViewById(R.id.recycler_reservas)
        tvNoReservas = findViewById(R.id.tv_no_reservas)
        btnVolver = findViewById(R.id.btn_volver)
    }

    private fun setupMonumentoData() {
        monumentoActual?.let { monumento ->
            // Cargar imagen del monumento
            Glide.with(this)
                .load(monumento.foto)
                .placeholder(R.drawable.imagen_1)
                .error(R.drawable.imagen_1)
                .centerCrop()
                .into(imgMonumento)

            // Configurar textos
            tvNombreMonumento.text = monumento.nombre?.uppercase() ?: "MONUMENTO DESCONOCIDO"
            tvUbicacionMonumento.text = monumento.ubicacion ?: "Ubicación no disponible"
            tvInfoMonumento.text = "Selecciona una reserva de hotel para guardar este monumento:"
        }
    }

    private fun setupRecyclerView() {
        reservasAdapter = ReservasSeleccionAdapter(reservasDisponibles) { reserva ->
            reservaSeleccionada = reserva
            // Actualizar el adaptador para mostrar la selección
            reservasAdapter.setReservaSeleccionada(reserva)
        }

        recyclerReservas.apply {
            layoutManager = LinearLayoutManager(this@GuardarMonumentoActivity)
            adapter = reservasAdapter
        }
    }

    private fun loadReservasHoteles() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            println("ERROR: Usuario no autenticado")
            showLoading(false)
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        database.reference
            .child("reservasHoteles")
            .child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    println("=== CARGA DE RESERVAS HOTELES ===")
                    println("Usuario ID: $userId")
                    println("Monumento ubicación: '${monumentoActual?.ubicacion}'")
                    println("Total reservas en Firebase: ${snapshot.childrenCount}")

                    reservasDisponibles.clear()

                    if (!snapshot.exists()) {
                        println("No existen reservas para este usuario")
                        showLoading(false)
                        updateUI()
                        return
                    }

                    var totalReservas = 0
                    var reservasValidas = 0

                    for (reservaSnapshot in snapshot.children) {
                        totalReservas++
                        println("Procesando reserva key: ${reservaSnapshot.key}")

                        try {
                            val reserva = reservaSnapshot.getValue(ReservaHotel::class.java)

                            if (reserva == null) {
                                println("  ❌ Error al deserializar reserva ${reservaSnapshot.key}")
                                continue
                            }

                            println("  Reserva ID: ${reserva.id}")
                            println("  Hotel: ${reserva.hotel?.nombre ?: "Sin hotel"}")
                            println("  Destino: '${reserva.destino}'")
                            println("  Hotel ubicación: '${reserva.hotel?.ubicacion ?: "Sin ubicación"}'")
                            println("  Estado: '${reserva.estado}'")

                            // Verificar que la reserva esté activa
                            val estadoValido = reserva.estado.equals("Confirmada", ignoreCase = true) ||
                                    reserva.estado.equals("Pendiente", ignoreCase = true)

                            println("  Estado válido: $estadoValido")

                            if (!estadoValido) {
                                println("  ❌ Reserva filtrada por estado inválido")
                                continue
                            }

                            // Obtener la ubicación desde diferentes fuentes posibles
                            val ubicacionReserva = obtenerUbicacionReserva(reserva)
                            println("  Ubicación final para comparar: '$ubicacionReserva'")

                            // Verificar compatibilidad de ubicación
                            val ubicacionCompatible = esUbicacionCompatible(
                                ubicacionReserva,
                                monumentoActual?.ubicacion
                            )

                            println("  Ubicación compatible: $ubicacionCompatible")

                            if (ubicacionCompatible) {
                                reservasDisponibles.add(reserva)
                                reservasValidas++
                                println("  ✅ Reserva agregada a la lista")
                            } else {
                                println("  ❌ Reserva filtrada por ubicación incompatible")
                            }

                        } catch (e: Exception) {
                            println("  ❌ Error procesando reserva ${reservaSnapshot.key}: ${e.message}")
                        }
                    }

                    println("Resumen final: $reservasValidas/$totalReservas reservas válidas")
                    println("Reservas disponibles: ${reservasDisponibles.size}")
                    println("=== FIN CARGA RESERVAS ===")

                    showLoading(false)
                    updateUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    println("❌ Error al cargar reservas: ${error.message}")
                    showLoading(false)
                    Toast.makeText(
                        this@GuardarMonumentoActivity,
                        "Error al cargar reservas: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun obtenerUbicacionReserva(reserva: ReservaHotel): String {
        // Intentar obtener la ubicación desde diferentes fuentes
        return when {
            // 1. Prioridad al destino si no está vacío
            !reserva.destino.isNullOrBlank() -> reserva.destino

            // 2. Si no hay destino, usar la ubicación del hotel
            !reserva.hotel?.ubicacion.isNullOrBlank() -> reserva.hotel!!.ubicacion!!

            // 3. Si tampoco hay ubicación del hotel, devolver vacío
            else -> ""
        }
    }

    private fun esUbicacionCompatible(destinoReserva: String?, ubicacionMonumento: String?): Boolean {
        // Verificar que ambas cadenas no sean nulas o vacías
        if (destinoReserva.isNullOrBlank() || ubicacionMonumento.isNullOrBlank()) {
            println("    DEBUG: Una de las ubicaciones está vacía")
            println("      Destino reserva: '${destinoReserva ?: "null"}'")
            println("      Ubicación monumento: '${ubicacionMonumento ?: "null"}'")
            return false
        }

        // Normalizar las cadenas
        val destino = normalizarUbicacion(destinoReserva)
        val ubicacion = normalizarUbicacion(ubicacionMonumento)

        println("    DEBUG: Comparando ubicaciones:")
        println("      Destino normalizado: '$destino'")
        println("      Ubicación normalizada: '$ubicacion'")

        // 1. Coincidencia exacta
        if (destino == ubicacion) {
            println("    ✅ Coincidencia exacta")
            return true
        }

        // 2. Una contiene a la otra (más permisivo)
        if (destino.contains(ubicacion) || ubicacion.contains(destino)) {
            println("    ✅ Una ubicación contiene a la otra")
            return true
        }

        // 3. Comparar por palabras individuales
        val palabrasDestino = destino.split(" ", ",", "-", "_").filter { it.length > 2 }
        val palabrasUbicacion = ubicacion.split(" ", ",", "-", "_").filter { it.length > 2 }

        println("    DEBUG: Palabras destino: $palabrasDestino")
        println("    DEBUG: Palabras ubicación: $palabrasUbicacion")

        for (palabraDestino in palabrasDestino) {
            for (palabraUbicacion in palabrasUbicacion) {
                if (palabraDestino.contains(palabraUbicacion) ||
                    palabraUbicacion.contains(palabraDestino)) {
                    println("    ✅ Coincidencia por palabras: '$palabraDestino' ~ '$palabraUbicacion'")
                    return true
                }
            }
        }

        // 4. Verificación más flexible - ciudades principales
        val ciudadesConocidas = listOf(
            "madrid", "barcelona", "sevilla", "valencia", "bilbao",
            "granada", "cordoba", "toledo", "salamanca", "santiago"
        )

        for (ciudad in ciudadesConocidas) {
            if ((destino.contains(ciudad) && ubicacion.contains(ciudad))) {
                println("    ✅ Coincidencia por ciudad conocida: $ciudad")
                return true
            }
        }

        println("    ❌ Sin coincidencias encontradas")
        return false
    }

    private fun normalizarUbicacion(ubicacion: String): String {
        return ubicacion
            .lowercase()
            .trim()
            // Normalizar caracteres especiales
            .replace("á", "a").replace("à", "a").replace("â", "a").replace("ä", "a")
            .replace("é", "e").replace("è", "e").replace("ê", "e").replace("ë", "e")
            .replace("í", "i").replace("ì", "i").replace("î", "i").replace("ï", "i")
            .replace("ó", "o").replace("ò", "o").replace("ô", "o").replace("ö", "o")
            .replace("ú", "u").replace("ù", "u").replace("û", "u").replace("ü", "u")
            .replace("ñ", "n").replace("ç", "c")
            // Eliminar múltiples espacios
            .replace(Regex("\\s+"), " ")
            // Eliminar caracteres especiales comunes
            .replace(Regex("[.,;:()\\[\\]{}\"'`]"), "")
    }

    private fun updateUI() {
        if (reservasDisponibles.isEmpty()) {
            recyclerReservas.visibility = View.GONE
            tvNoReservas.visibility = View.VISIBLE
            tvNoReservas.text = "No tienes reservas de hotel activas en esta ubicación.\n\nPara guardar este monumento, necesitas tener una reserva de hotel en ${monumentoActual?.ubicacion}.\n\nVerifica que tus reservas estén confirmadas o pendientes."
        } else {
            recyclerReservas.visibility = View.VISIBLE
            tvNoReservas.visibility = View.GONE
            reservasAdapter.notifyDataSetChanged()
        }
    }

    private fun setupClickListeners() {
        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun guardarMonumento() {
        val reserva = reservaSeleccionada ?: return
        val monumento = monumentoActual ?: return
        val userId = auth.currentUser?.uid ?: return

        // Mostrar diálogo de confirmación
        AlertDialog.Builder(this)
            .setTitle("Guardar monumento")
            .setMessage("¿Deseas guardar '${monumento.nombre}' en tu reserva del hotel '${reserva.hotel?.nombre}'?")
            .setPositiveButton("Guardar") { _, _ ->
                procederConGuardado(userId, reserva, monumento)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun procederConGuardado(userId: String, reserva: ReservaHotel, monumento: Monumento) {
        // Mostrar loading
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Crear referencia a los monumentos guardados de la reserva
        val monumentosRef = database.reference
            .child("reservasHoteles")
            .child(userId)
            .child(reserva.id)
            .child("monumentosGuardados")

        // Obtener monumentos existentes y agregar el nuevo
        monumentosRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val monumentosExistentes = mutableListOf<Monumento>()

                // Obtener monumentos ya guardados
                for (monumentoSnapshot in snapshot.children) {
                    val monumentoGuardado = monumentoSnapshot.getValue(Monumento::class.java)
                    monumentoGuardado?.let { monumentosExistentes.add(it) }
                }

                // Verificar si el monumento ya está guardado
                val yaExiste = monumentosExistentes.any { it.id == monumento.id }

                if (yaExiste) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@GuardarMonumentoActivity,
                        "Este monumento ya está guardado en esta reserva",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                // Agregar el nuevo monumento
                monumentosExistentes.add(monumento)

                // Guardar la lista actualizada
                monumentosRef.setValue(monumentosExistentes)
                    .addOnCompleteListener { task ->
                        loadingDialog.dismiss()

                        if (task.isSuccessful) {
                            Toast.makeText(
                                this@GuardarMonumentoActivity,
                                "Monumento guardado exitosamente",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        } else {
                            Toast.makeText(
                                this@GuardarMonumentoActivity,
                                "Error al guardar el monumento: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                loadingDialog.dismiss()
                Toast.makeText(
                    this@GuardarMonumentoActivity,
                    "Error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            recyclerReservas.visibility = View.GONE
            tvNoReservas.visibility = View.VISIBLE
            tvNoReservas.text = "Cargando reservas..."
        }
    }

    // Función para ser llamada desde el adaptador cuando se selecciona una reserva
    fun onReservaSeleccionada(reserva: ReservaHotel) {
        reservaSeleccionada = reserva
        guardarMonumento()
    }
}