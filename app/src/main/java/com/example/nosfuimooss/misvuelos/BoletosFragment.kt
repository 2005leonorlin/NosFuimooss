package com.example.nosfuimooss.misvuelos

import android.content.Intent

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.NosFuimooss.R

import com.example.nosfuimooss.usuario.Reserva
import com.example.nosfuimooss.usuariologeado.DetalleVuelos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
class BoletosFragment : Fragment() {

    private lateinit var recyclerBoletos: RecyclerView
    private lateinit var tvNoBoletos: TextView
    private lateinit var boletosAdapter: BoletosAdapter
    private var listaReservas = mutableListOf<Reserva>()

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_boletos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Inicializar vistas
        initViews(view)
        setupRecyclerView()
        cargarBoletos()
    }

    private fun initViews(view: View) {
        recyclerBoletos = view.findViewById(R.id.recycler_boletos)
        tvNoBoletos = view.findViewById(R.id.tv_no_boletos)
    }

    private fun setupRecyclerView() {
        boletosAdapter = BoletosAdapter(listaReservas) { reserva ->
            mostrarDetallesReserva(reserva)
        }

        recyclerBoletos.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = boletosAdapter
        }
    }

    private fun cargarBoletos() {
        val userId = auth.currentUser?.uid ?: return

        showLoading(true)

        val reservasRef = database.reference.child("reservas").child(userId)

        reservasRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaReservas.clear()

                for (reservaSnapshot in snapshot.children) {
                    val reserva = reservaSnapshot.getValue(Reserva::class.java)
                    reserva?.let {
                        if (it.estado == "Confirmada") {
                            listaReservas.add(it)
                        }
                    }
                }

                // Ordenar por fecha más reciente
                listaReservas.sortByDescending { it.fechaReserva }

                actualizarUI()
                showLoading(false)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BoletosFragment", "Error al cargar boletos: ${error.message}")
                Toast.makeText(context, "Error al cargar tus boletos", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        })
    }

    private fun actualizarUI() {
        if (listaReservas.isEmpty()) {
            recyclerBoletos.visibility = View.GONE
            tvNoBoletos.visibility = View.VISIBLE
        } else {
            recyclerBoletos.visibility = View.VISIBLE
            tvNoBoletos.visibility = View.GONE
            boletosAdapter.notifyDataSetChanged()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            recyclerBoletos.visibility = View.INVISIBLE
        } else {
            if (listaReservas.isNotEmpty()) {
                recyclerBoletos.visibility = View.VISIBLE
            }
        }
    }

    private fun mostrarDetallesReserva(reserva: Reserva) {
        reserva.boletoIda?.let { boleto ->
            val destinoId = boleto.id
            if (destinoId.isNotEmpty()) {
                val intent = Intent(context, DetalleVuelos::class.java)
                intent.putExtra("destinoId", destinoId)
                startActivity(intent)
            }
        }
    }

    // Adapter para boletos
    inner class BoletosAdapter(
        private val reservas: List<Reserva>,
        private val onItemClick: (Reserva) -> Unit
    ) : RecyclerView.Adapter<BoletosAdapter.BoletoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoletoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_viaje, parent, false)
            return BoletoViewHolder(view)
        }

        override fun getItemCount() = reservas.size

        override fun onBindViewHolder(holder: BoletoViewHolder, position: Int) {
            val reserva = reservas[position]
            holder.bind(reserva)
        }

        inner class BoletoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imgDestino = itemView.findViewById<android.widget.ImageView>(R.id.image_destino)
            private val textDestino = itemView.findViewById<TextView>(R.id.text_destino)
            private val textOrigenDestino = itemView.findViewById<TextView>(R.id.text_origen_destino)

            // Elementos del vuelo de ida
            private val textIdaTitle = itemView.findViewById<TextView>(R.id.text_ida_title)
            private val textIdaDetalle = itemView.findViewById<TextView>(R.id.text_ida_detalle)
            private val textIdaFechaHora = itemView.findViewById<TextView>(R.id.text_ida_fecha_hora)
            private val textIdaSalida = itemView.findViewById<TextView>(R.id.text_ida_salida)
            private val textIdaSalidaHora = itemView.findViewById<TextView>(R.id.text_ida_salida_hora)

            // Elementos del vuelo de vuelta
            private val textVueltaTitle = itemView.findViewById<TextView>(R.id.text_vuelta_title)
            private val textVueltaDetalle = itemView.findViewById<TextView>(R.id.text_vuelta_detalle)
            private val textVueltaFechaHora = itemView.findViewById<TextView>(R.id.text_vuelta_fecha_hora)
            private val textVueltaSalida = itemView.findViewById<TextView>(R.id.text_vuelta_salida)
            private val textVueltaSalidaHora = itemView.findViewById<TextView>(R.id.text_vuelta_salida_hora)

            private val btnVerVuelo = itemView.findViewById<CardView>(R.id.btn_ver_vuelo)

            fun bind(reserva: Reserva) {
                // Configurar información básica
                textDestino.text = reserva.boletoIda?.destino ?: "Destino desconocido"
                textOrigenDestino.text = "${reserva.boletoIda?.origen ?: "?"} → ${reserva.boletoIda?.destino ?: "?"}"

                // Formatear fecha
                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val idaDate = if (reserva.fechaIda > 0) {
                    dateFormat.format(java.util.Date(reserva.fechaIda))
                } else {
                    "Fecha desconocida"
                }
                textIdaTitle.text = "Ida $idaDate"

                // Configurar detalles de ida
                textIdaDetalle.text = "Vuelo a ${reserva.boletoIda?.destino}"
                textIdaFechaHora.text = if (reserva.idaArrivalTime.isNotEmpty())
                    reserva.idaArrivalTime else "Hora desconocida"
                textIdaSalida.text = "Salida:"
                textIdaSalidaHora.text = if (reserva.idaDepartureTime.isNotEmpty())
                    reserva.idaDepartureTime else "Hora desconocida"

                // Configurar vuelo de vuelta si existe
                if (reserva.roundTrip) {
                    textVueltaTitle.visibility = View.VISIBLE
                    textVueltaDetalle.visibility = View.VISIBLE
                    textVueltaFechaHora.visibility = View.VISIBLE
                    textVueltaSalida.visibility = View.VISIBLE
                    textVueltaSalidaHora.visibility = View.VISIBLE

                    val vueltaDate = if (reserva.fechaVuelta > 0) {
                        dateFormat.format(java.util.Date(reserva.fechaVuelta))
                    } else {
                        "Fecha desconocida"
                    }
                    textVueltaTitle.text = "Vuelta $vueltaDate"

                    val destinoVuelta = if (reserva.boletoVuelta != null && reserva.boletoVuelta.destino.isNotEmpty()) {
                        reserva.boletoVuelta.destino
                    } else {
                        reserva.boletoIda?.origen ?: "Destino desconocido"
                    }

                    textVueltaDetalle.text = "Vuelo a $destinoVuelta"
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
                } else {
                    // Ocultar sección de vuelta
                    textVueltaTitle.visibility = View.GONE
                    textVueltaDetalle.visibility = View.GONE
                    textVueltaFechaHora.visibility = View.GONE
                    textVueltaSalida.visibility = View.GONE
                    textVueltaSalidaHora.visibility = View.GONE
                }

                // Cargar imagen
                if (reserva.imageUrl.isNotEmpty()) {
                    com.bumptech.glide.Glide.with(itemView.context)
                        .load(reserva.imageUrl)
                        .centerCrop()
                        .into(imgDestino)
                } else {
                    imgDestino.setImageResource(R.drawable.imagen_1)
                }

                // Configurar click del botón
                btnVerVuelo.setOnClickListener {
                    val context = itemView.context
                    val intent = Intent(context, DetalleReservaActivity::class.java)
                    intent.putExtra("reserva", reserva)
                    context.startActivity(intent)
                }
            }
        }
    }
}