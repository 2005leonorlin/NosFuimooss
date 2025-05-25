package com.example.nosfuimooss.Adapter.hotelyvuelo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Hotel

class ElegirHotelAdapter(
    private var hoteles: List<Hotel>,
    private val onClick: (Hotel) -> Unit,
    private var adultos: Int = 1,
    private var ninos: Int = 0
) : RecyclerView.Adapter<ElegirHotelAdapter.ElegirHotelViewHolder>() {

    inner class ElegirHotelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivHotelImage: ImageView = view.findViewById(R.id.ivHotelImage)
        val tvHotelName: TextView = view.findViewById(R.id.tvHotelName)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvAvailability: TextView = view.findViewById(R.id.tvAvailability)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val btnReservar: Button = view.findViewById(R.id.btnReservar)
    }

    fun actualizarCantidadPersonas(nuevosAdultos: Int, nuevosNinos: Int) {
        adultos = nuevosAdultos
        ninos = nuevosNinos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElegirHotelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hotel, parent, false)
        return ElegirHotelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ElegirHotelViewHolder, position: Int) {
        val hotel = hoteles[position]

        holder.tvHotelName.text = hotel.nombre
        holder.tvLocation.text = hotel.ubicacion

        val totalPersonas = adultos + ninos
        val adicionales = (totalPersonas - 2).coerceAtLeast(0)
        val precioFinal = hotel.precioNoche + (adicionales * 22)
        holder.tvPrice.text = "Desde ${precioFinal} € / noche"

        // Mostrar disponibilidad
        holder.tvAvailability.text = if (hotel.disponible) "Disponible" else "No disponible"
        holder.tvAvailability.setBackgroundResource(
            if (hotel.disponible) R.drawable.bg_available else R.drawable.bg_unavailable
        )

        // Estrellas como float
        holder.ratingBar.rating = hotel.estrellas.toFloat()

        // Cargar imagen
        Glide.with(holder.itemView.context)
            .load(hotel.imagenUrl)
            .placeholder(R.drawable.placeholder_image)
            .into(holder.ivHotelImage)




        // Configurar el evento click específico para el botón "Ver hotel"
        holder.btnReservar.setOnClickListener {

            onClick(hotel)
        }
    }

    fun getHoteles(): List<Hotel> {
        return hoteles
    }

    override fun getItemCount(): Int = hoteles.size

    fun updateData(nuevosHoteles: List<Hotel>) {
        hoteles = nuevosHoteles
        notifyDataSetChanged()
    }
}