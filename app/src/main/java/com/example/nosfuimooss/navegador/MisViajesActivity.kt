package com.example.nosfuimooss.navegador

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.NosFuimooss.R
import com.example.nosfuimooss.misvuelos.DetalleReservaActivity
import com.example.nosfuimooss.model.Reserva
import com.example.nosfuimooss.usuariologeado.DetalleVuelos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MisViajesActivity : AppCompatActivity() {

    private lateinit var recyclerMisViajes: RecyclerView
    private lateinit var tvNoViajes: TextView
    private lateinit var viajesAdapter: ViajesAdapter
    private var listaReservas = mutableListOf<Reserva>()

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var nuevaReservaId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_mis_viajes)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Verificar si hay usuario logueado
        if (auth.currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para ver tus viajes", Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }

        // Inicializar vistas
        initViews()

        // Configurar RecyclerView
        setupRecyclerView()

        // Cargar los viajes del usuario
        cargarViajes()

        // Configurar navegación inferior
        setupBottomNavigation()
        loadUserInfo()

        // Verificar si llegamos de una nueva reserva
        nuevaReservaId = intent.getStringExtra("nuevaReservaId")
    }

    private fun initViews() {

        recyclerMisViajes = findViewById(R.id.recycler_mis_viajes)

        // Añadir TextView para mostrar mensaje cuando no hay viajes
        // Asumimos que lo añadiremos al layout si no existe
        tvNoViajes = TextView(this).apply {
            text = "No tienes viajes reservados"
            textSize = 18f
            visibility = View.GONE
        }

        // Configurar listener para los botones de navegación superior
        findViewById<View>(R.id.ic_search).setOnClickListener {
            Toast.makeText(this, "Búsqueda no implementada", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.ic_calendar).setOnClickListener {
            Toast.makeText(this, "Calendario no implementado", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.ic_logo).setOnClickListener {
            // Volver a la pantalla principal
            finish()
        }

    }

    private fun setupRecyclerView() {
        viajesAdapter = ViajesAdapter(listaReservas) { reserva ->
            // OnClick para cada viaje, podríamos mostrar detalles
            mostrarDetallesViaje(reserva)
        }

        recyclerMisViajes.apply {
            layoutManager = LinearLayoutManager(this@MisViajesActivity)
            adapter = viajesAdapter
        }
    }
    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userNameText = findViewById<TextView>(R.id.user_name_text)
            val database = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(userId)

            database.get().addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(com.example.nosfuimooss.model.User::class.java)
                if (user != null) {
                    userNameText.text = " ${user.name} "
                } else {
                    userNameText.text = "Usuario"
                }
            }.addOnFailureListener {
                userNameText.text = "Usuario"
            }
        }
    }
    private fun cargarViajes() {
        val userId = auth.currentUser?.uid ?: return

        // Mostrar carga
        showLoading(true)

        // Referencia a las reservas del usuario actual
        val reservasRef = database.reference.child("reservas").child(userId)

        reservasRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaReservas.clear()

                for (reservaSnapshot in snapshot.children) {
                    val reserva = reservaSnapshot.getValue(Reserva::class.java)
                    reserva?.let {
                        // Añadir debug log para identificar el problema
                        Log.d(
                            "MisViajesActivity",
                            "Cargada reserva: ${it.id}, isRoundTrip: ${it.roundTrip}, " +
                                    "boletoVuelta: ${it.boletoVuelta != null}, " +
                                    "fechaVuelta: ${it.fechaVuelta}, " +
                                    "vueltaDeparture: ${it.vueltaDepartureTime}, " +
                                    "vueltaArrival: ${it.vueltaArrivalTime}"
                        )

                        // Solo mostramos las reservas confirmadas
                        if (it.estado == "Confirmada") {
                            listaReservas.add(it)

                            // Si esta es la nueva reserva, la resaltamos
                            if (it.id == nuevaReservaId) {
                                scrollToReserva(it)
                            }
                        }
                    }
                }

                // Ordenar por fecha más reciente
                listaReservas.sortByDescending { it.fechaReserva }

                // Actualizar UI
                actualizarUI()
                showLoading(false)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MisViajesActivity", "Error al cargar viajes: ${error.message}")
                Toast.makeText(
                    this@MisViajesActivity,
                    "Error al cargar tus viajes",
                    Toast.LENGTH_SHORT
                ).show()
                showLoading(false)
            }
        })
    }

    private fun actualizarUI() {
        if (listaReservas.isEmpty()) {
            recyclerMisViajes.visibility = View.GONE
            tvNoViajes.visibility = View.VISIBLE

            // Añadir el TextView al layout si no está ya añadido
            val parent = recyclerMisViajes.parent as? android.view.ViewGroup
            if (tvNoViajes.parent == null) {
                parent?.addView(tvNoViajes)

                // Centrar el TextView
                val params = tvNoViajes.layoutParams as? android.view.ViewGroup.LayoutParams
                if (params is android.widget.RelativeLayout.LayoutParams) {
                    params.addRule(
                        android.widget.RelativeLayout.CENTER_IN_PARENT,
                        android.widget.RelativeLayout.TRUE
                    )
                } else if (params is androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
                    params.topToTop =
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    params.bottomToBottom =
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    params.startToStart =
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    params.endToEnd =
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                }
                tvNoViajes.layoutParams = params
            }
        } else {
            recyclerMisViajes.visibility = View.VISIBLE
            tvNoViajes.visibility = View.GONE
            viajesAdapter.notifyDataSetChanged()
        }
    }

    private fun setupBottomNavigation() {
        // Home
        findViewById<View>(R.id.nav_home).setOnClickListener {
            finish() // Volver a la pantalla principal
        }

        // Vuelos
        findViewById<View>(R.id.nav_flight).setOnClickListener {
            // Implementar navegación a búsqueda de vuelos
            Toast.makeText(this, "Búsqueda de vuelos", Toast.LENGTH_SHORT).show()
        }

        // Maleta (Mis viajes) - Ya estamos aquí
        findViewById<View>(R.id.nav_moon).setOnClickListener {
            // Estamos en esta pantalla, no hacemos nada
        }

        // Favoritos
        findViewById<View>(R.id.nav_heart).setOnClickListener {
            Toast.makeText(this, "Favoritos no implementado", Toast.LENGTH_SHORT).show()
        }

        // Perfil
        findViewById<View>(R.id.nav_profile).setOnClickListener {
            Toast.makeText(this, "Perfil no implementado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        // Implementar un ProgressBar si es necesario
        // Por ahora simplemente actualizamos la visibilidad del RecyclerView
        if (isLoading) {
            recyclerMisViajes.visibility = View.INVISIBLE
        } else {
            if (listaReservas.isNotEmpty()) {
                recyclerMisViajes.visibility = View.VISIBLE
            }
        }
    }

    private fun scrollToReserva(reserva: Reserva) {
        val position = listaReservas.indexOf(reserva)
        if (position != -1) {
            recyclerMisViajes.smoothScrollToPosition(position)

            // Opcional: Resaltar visualmente la reserva
            viajesAdapter.setHighlightedPosition(position)
        }
    }

    private fun mostrarDetallesViaje(reserva: Reserva) {
        // Implementar navegación a detalles del viaje
        // Por ejemplo, podríamos mostrar una nueva actividad con más detalles
        Toast.makeText(
            this,
            "Detalles del viaje a ${reserva.boletoIda?.destino}",
            Toast.LENGTH_SHORT
        ).show()

        // Si hay un ID de destino, podríamos navegar a DetalleVuelos
        reserva.boletoIda?.let { boleto ->
            val destinoId = boleto.id
            if (destinoId.isNotEmpty()) {
                val intent = Intent(this, DetalleVuelos::class.java)
                intent.putExtra("destinoId", destinoId)
                startActivity(intent)
            }
        }
    }

    // Clase adaptador para el RecyclerView
    inner class ViajesAdapter(
        private val reservas: List<Reserva>,
        private val onItemClick: (Reserva) -> Unit
    ) : RecyclerView.Adapter<ViajesAdapter.ViajeViewHolder>() {

        private var highlightedPosition = -1

        fun setHighlightedPosition(position: Int) {
            val oldPosition = highlightedPosition
            highlightedPosition = position

            // Actualizar las vistas
            if (oldPosition != -1) notifyItemChanged(oldPosition)
            notifyItemChanged(position)

            // Resetear después de un tiempo
            android.os.Handler().postDelayed({
                highlightedPosition = -1
                notifyItemChanged(position)
            }, 2000)
        }

        override fun onCreateViewHolder(
            parent: android.view.ViewGroup,
            viewType: Int
        ): ViajeViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_viaje, parent, false)
            return ViajeViewHolder(view)
        }

        override fun getItemCount() = reservas.size

        override fun onBindViewHolder(holder: ViajeViewHolder, position: Int) {
            val reserva = reservas[position]
            holder.bind(reserva, position == highlightedPosition)
        }

        inner class ViajeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imgDestino =
                itemView.findViewById<android.widget.ImageView>(R.id.image_destino)
            private val textDestino = itemView.findViewById<TextView>(R.id.text_destino)
            private val textOrigenDestino =
                itemView.findViewById<TextView>(R.id.text_origen_destino)

            // Referencias a los elementos del vuelo de ida
            private val textIdaTitle = itemView.findViewById<TextView>(R.id.text_ida_title)
            private val textIdaDetalle = itemView.findViewById<TextView>(R.id.text_ida_detalle)
            private val textIdaFechaHora = itemView.findViewById<TextView>(R.id.text_ida_fecha_hora)
            private val textIdaSalida = itemView.findViewById<TextView>(R.id.text_ida_salida)
            private val textIdaSalidaHora =
                itemView.findViewById<TextView>(R.id.text_ida_salida_hora)

            // Referencias a los elementos del vuelo de vuelta
            private val textVueltaTitle = itemView.findViewById<TextView>(R.id.text_vuelta_title)
            private val textVueltaDetalle =
                itemView.findViewById<TextView>(R.id.text_vuelta_detalle)
            private val textVueltaFechaHora =
                itemView.findViewById<TextView>(R.id.text_vuelta_fecha_hora)
            private val textVueltaSalida = itemView.findViewById<TextView>(R.id.text_vuelta_salida)
            private val textVueltaSalidaHora =
                itemView.findViewById<TextView>(R.id.text_vuelta_salida_hora)

            // Botón para ver vuelo
            private val btnVerVuelo =
                itemView.findViewById<androidx.cardview.widget.CardView>(R.id.btn_ver_vuelo)

            fun bind(reserva: Reserva, isHighlighted: Boolean) {
                // Log para depuración de vuelo ida y vuelta (mantener para diagnóstico)
                Log.d(
                    "ViajeViewHolder", "Binding reserva ${reserva.id}: " +
                            "isRoundTrip=${reserva.roundTrip}, " +
                            "boletoVuelta=${reserva.boletoVuelta != null}, " +
                            "fechaVuelta=${reserva.fechaVuelta}, " +
                            "vueltaDepartureTime=${reserva.vueltaDepartureTime}, " +
                            "vueltaArrivalTime=${reserva.vueltaArrivalTime}"
                )

                // Establecer los datos de la reserva
                textDestino.text = reserva.boletoIda?.destino ?: "Destino desconocido"
                textOrigenDestino.text =
                    "${reserva.boletoIda?.origen ?: "?"} → ${reserva.boletoIda?.destino ?: "?"}"

                // Formatear fecha para el título de ida
                val dateFormat =
                    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val idaDate = if (reserva.fechaIda > 0) {
                    dateFormat.format(java.util.Date(reserva.fechaIda))
                } else {
                    "Fecha desconocida"
                }
                textIdaTitle.text = "Ida $idaDate"

                // Establecer detalles de ida
                textIdaDetalle.text = "Vuelo a ${reserva.boletoIda?.destino}"

                // Establecer horarios de ida - Verificando que no estén vacíos
                textIdaFechaHora.text = if (reserva.idaArrivalTime.isNotEmpty())
                    reserva.idaArrivalTime else "Hora desconocida"

                textIdaSalida.text = "Salida:"
                textIdaSalidaHora.text = if (reserva.idaDepartureTime.isNotEmpty())
                    reserva.idaDepartureTime else "Hora desconocida"

                // SOLUCIÓN CORREGIDA: Mejorada la lógica para mostrar vuelo de vuelta
                // Verificamos primero el campo isRoundTrip que debería ser el principal indicador
                if (reserva.roundTrip) {
                    // Log adicional para diagnóstico
                    Log.d("ViajeViewHolder", "Es vuelo de ida y vuelta según isRoundTrip")

                    // Habilitar visibilidad de la sección de vuelta
                    textVueltaTitle.visibility = View.VISIBLE
                    textVueltaDetalle.visibility = View.VISIBLE
                    textVueltaFechaHora.visibility = View.VISIBLE
                    textVueltaSalida.visibility = View.VISIBLE
                    textVueltaSalidaHora.visibility = View.VISIBLE

                    // Formatear fecha para el título de vuelta
                    val vueltaDate = if (reserva.fechaVuelta > 0) {
                        dateFormat.format(java.util.Date(reserva.fechaVuelta))
                    } else {
                        "Fecha desconocida"
                    }
                    textVueltaTitle.text = "Vuelta $vueltaDate"

                    // Establecer detalles de vuelta - Usando la información disponible con fallbacks
                    val origenVuelta =
                        if (reserva.boletoVuelta != null && reserva.boletoVuelta.origen.isNotEmpty()) {
                            reserva.boletoVuelta.origen
                        } else {
                            reserva.boletoIda?.destino ?: "Origen desconocido"
                        }

                    val destinoVuelta =
                        if (reserva.boletoVuelta != null && reserva.boletoVuelta.destino.isNotEmpty()) {
                            reserva.boletoVuelta.destino
                        } else {
                            reserva.boletoIda?.origen ?: "Destino desconocido"
                        }

                    textVueltaDetalle.text = "Vuelo a $destinoVuelta"

                    // Establecer horarios de vuelta - Con valores por defecto si están vacíos
                    textVueltaFechaHora.text = if (reserva.vueltaArrivalTime.isNotEmpty()) {
                        reserva.vueltaArrivalTime
                    } else {
                        "Hora desconocida"
                    }

                    textVueltaSalida.text = "Salida:"
                    textVueltaSalidaHora.text = if (reserva.vueltaDepartureTime.isNotEmpty()) {
                        reserva.vueltaDepartureTime
                    } else {
                        "Hora desconocida"
                    }

                    // Log adicional para verificar datos de vuelta mostrados
                    Log.d(
                        "ViajeViewHolder", "Mostrando vuelo de vuelta: $vueltaDate, " +
                                "De $origenVuelta a $destinoVuelta, " +
                                "Salida: ${reserva.vueltaDepartureTime}, " +
                                "Llegada: ${reserva.vueltaArrivalTime}"
                    )
                } else {
                    // Ocultar sección de vuelta
                    textVueltaTitle.visibility = View.GONE
                    textVueltaDetalle.visibility = View.GONE
                    textVueltaFechaHora.visibility = View.GONE
                    textVueltaSalida.visibility = View.GONE
                    textVueltaSalidaHora.visibility = View.GONE

                    Log.d("ViajeViewHolder", "Ocultando sección de vuelta - No es ida y vuelta")
                }

                // Cargar imagen del destino
                if (reserva.imageUrl.isNotEmpty()) {
                    com.bumptech.glide.Glide.with(itemView.context)
                        .load(reserva.imageUrl)
                        .centerCrop()
                        .into(imgDestino)
                } else {
                    // Imagen por defecto
                    imgDestino.setImageResource(R.drawable.imagen_1)
                }

                // Resaltar si es la nueva reserva
                if (isHighlighted) {
                    itemView.setBackgroundColor(android.graphics.Color.parseColor("#F5E6FF")) // Un color lila muy claro

                    // Animación de pulsación
                    itemView.animate()
                        .scaleX(1.02f)
                        .scaleY(1.02f)
                        .setDuration(300)
                        .withEndAction {
                            itemView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(300)
                                .start()
                        }
                        .start()
                } else {
                    (itemView as CardView).setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                    itemView.scaleX = 1.0f
                    itemView.scaleY = 1.0f
                }

                // Configurar botón y click en el elemento
                btnVerVuelo.setOnClickListener {
                    val context = itemView.context
                    val intent = Intent(
                        context, DetalleReservaActivity::class.java
                    )
                    intent.putExtra(
                        "reserva",
                        reserva
                    ) // Asegúrate que Reserva implementa Serializable
                    context.startActivity(intent)
                }

            }
        }
    }
}