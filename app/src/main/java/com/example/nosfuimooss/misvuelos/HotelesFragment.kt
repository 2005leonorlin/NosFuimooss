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
import com.example.nosfuimooss.usuario.ReservaHotel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class HotelesFragment : Fragment() {

    private lateinit var recyclerHoteles: RecyclerView
    private lateinit var tvNoHoteles: TextView
    private lateinit var hotelesAdapter: HotelesAdapter
    private var listaHoteles = mutableListOf<ReservaHotel>()

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hoteles, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Inicializar vistas
        initViews(view)
        setupRecyclerView()
        cargarHoteles()
    }

    private fun initViews(view: View) {
        recyclerHoteles = view.findViewById(R.id.recycler_hoteles)
        tvNoHoteles = view.findViewById(R.id.tv_no_hoteles)
    }

    private fun setupRecyclerView() {
        hotelesAdapter = HotelesAdapter(listaHoteles) { hotel ->
            mostrarDetallesHotel(hotel)
        }

        recyclerHoteles.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = hotelesAdapter
        }
    }

    private fun cargarHoteles() {
        val userId = auth.currentUser?.uid ?: return

        showLoading(true)

        // Cambiar la referencia para que coincida con ConfirmacionHotelesActivity
        val hotelesRef = database.reference.child("reservasHoteles").child(userId)

        hotelesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaHoteles.clear()

                for (hotelSnapshot in snapshot.children) {
                    val hotel = hotelSnapshot.getValue(ReservaHotel::class.java)
                    hotel?.let {
                        // Solo mostrar reservas confirmadas
                        if (it.estado == "Confirmada") {
                            listaHoteles.add(it)
                        }
                    }
                }

                // Ordenar por fecha de reserva más reciente
                listaHoteles.sortByDescending { it.fechaReserva }

                actualizarUI()
                showLoading(false)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HotelesFragment", "Error al cargar hoteles: ${error.message}")
                Toast.makeText(context, "Error al cargar tus hoteles", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        })
    }

    private fun actualizarUI() {
        if (listaHoteles.isEmpty()) {
            recyclerHoteles.visibility = View.GONE
            tvNoHoteles.visibility = View.VISIBLE
        } else {
            recyclerHoteles.visibility = View.VISIBLE
            tvNoHoteles.visibility = View.GONE
            hotelesAdapter.notifyDataSetChanged()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            recyclerHoteles.visibility = View.INVISIBLE
        } else {
            if (listaHoteles.isNotEmpty()) {
                recyclerHoteles.visibility = View.VISIBLE
            }
        }
    }

    private fun mostrarDetallesHotel(hotel: ReservaHotel) {
        // Navegar a la actividad de detalles de reserva de hotel
        val intent = Intent(context, DetalleReservaHotelActivity::class.java)
        intent.putExtra("reservaHotel", hotel)
        startActivity(intent)
    }

    // Adapter para hoteles
    inner class HotelesAdapter(
        private val hoteles: List<ReservaHotel>,
        private val onItemClick: (ReservaHotel) -> Unit
    ) : RecyclerView.Adapter<HotelesAdapter.HotelViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_mis_hoteles, parent, false)
            return HotelViewHolder(view)
        }

        override fun getItemCount() = hoteles.size

        override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
            val hotel = hoteles[position]
            holder.bind(hotel)
        }

        inner class HotelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageHotel = itemView.findViewById<android.widget.ImageView>(R.id.image_hotel)
            private val textHotelNombre = itemView.findViewById<TextView>(R.id.text_hotel_nombre)
            private val textHotelUbicacion = itemView.findViewById<TextView>(R.id.text_hotel_ubicacion)
            private val textRating = itemView.findViewById<TextView>(R.id.text_rating)
            private val textRatingNumber = itemView.findViewById<TextView>(R.id.text_rating_number)
            private val textCheckinFecha = itemView.findViewById<TextView>(R.id.text_checkin_fecha)
            private val textCheckoutFecha = itemView.findViewById<TextView>(R.id.text_checkout_fecha)
            private val textHabitacionTipo = itemView.findViewById<TextView>(R.id.text_habitacion_tipo)
            private val textNoches = itemView.findViewById<TextView>(R.id.text_noches)
            private val textHuespedes = itemView.findViewById<TextView>(R.id.text_huespedes)
            private val btnVerHotel = itemView.findViewById<CardView>(R.id.btn_ver_hotel)

            fun bind(hotel: ReservaHotel) {
                // Configurar información básica del hotel
                textHotelNombre.text = hotel.hotel?.nombre ?: "Hotel desconocido"
                textHotelUbicacion.text = hotel.hotel?.ubicacion ?: hotel.destino

                // Configurar rating
                val rating = hotel.hotel?.estrellas ?: 0
                val estrellas = "★".repeat(rating) + "☆".repeat(5 - rating)
                textRating.text = estrellas
                textRatingNumber.text = rating.toString()

                // Configurar fechas
                textCheckinFecha.text = hotel.fechaEntrada
                textCheckoutFecha.text = hotel.fechaSalida

                // Configurar detalles de la habitación
                textHabitacionTipo.text = hotel.tipoHabitacion
                textNoches.text = "${hotel.noches} noches"

                val totalHuespedes = hotel.adultos + hotel.ninos
                textHuespedes.text = "$totalHuespedes huéspedes"

                // Cargar imagen del hotel
                val imageUrl = hotel.hotel?.imagenUrl ?: ""
                if (imageUrl.isNotEmpty()) {
                    com.bumptech.glide.Glide.with(itemView.context)
                        .load(imageUrl)
                        .centerCrop()
                        .into(imageHotel)
                } else {
                    // Imagen por defecto para hoteles
                    imageHotel.setImageResource(R.drawable.imagen_1)
                }

                // Configurar click del botón
                btnVerHotel.setOnClickListener {
                    onItemClick(hotel)
                }
            }
        }
    }
}