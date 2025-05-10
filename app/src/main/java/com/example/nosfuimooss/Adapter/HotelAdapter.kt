package com.example.nosfuimooss.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Hotel

class HotelAdapter(
    private var hoteles: List<Hotel>
) : RecyclerView.Adapter<HotelAdapter.HotelViewHolder>() {

    // Listener opcional para click en hotel
    var onItemClick: ((Hotel) -> Unit)? = null

    inner class HotelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivHotelImage: ImageView = itemView.findViewById(R.id.ivHotelImage)
        private val tvHotelName: TextView = itemView.findViewById(R.id.tvHotelName)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvAvailability: TextView = itemView.findViewById(R.id.tvAvailability)

        fun bind(hotel: Hotel) {
            // Cargar imagen con Glide
            Glide.with(itemView.context)
                .load(hotel.imagen)
                .centerCrop()
                .into(ivHotelImage)

            tvHotelName.text = hotel.nombre
            ratingBar.rating = hotel.estrellas.toFloatOrNull() ?: 0f
            tvLocation.text = hotel.ubicacion
            tvPrice.text = "€${hotel.precioNoche}"
            tvAvailability.text = if (hotel.disponible)
                itemView.context.getString(R.string.disponible)
            else
                itemView.context.getString(R.string.no_disponible)

            itemView.setOnClickListener {
                onItemClick?.invoke(hotel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hotel, parent, false)
        return HotelViewHolder(view)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        holder.bind(hoteles[position])
    }

    override fun getItemCount(): Int = hoteles.size

    /**
     * Actualiza la lista de hoteles y notifica al adapter.
     */
    fun updateList(nuevaLista: List<Hotel>) {
        hoteles = nuevaLista
        notifyDataSetChanged()
    }

    /**
     * Agrega la funcionalidad para ordenar los hoteles.
     */
    fun ordenarPorPrecio() {
        hoteles = hoteles.sortedBy { it.precioNoche }
        notifyDataSetChanged()
    }

    fun ordenarPorEstrellas() {
        hoteles = hoteles.sortedByDescending { it.estrellas }
        notifyDataSetChanged()
    }
}
