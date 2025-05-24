package com.example.nosfuimooss.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.monumento.GuardarMonumentoActivity
import com.example.nosfuimooss.usuario.ReservaHotel

class ReservasSeleccionAdapter(
    private val reservas: List<ReservaHotel>,
    private val onReservaSelected: (ReservaHotel) -> Unit
) : RecyclerView.Adapter<ReservasSeleccionAdapter.ReservaViewHolder>() {

    private var reservaSeleccionada: ReservaHotel? = null

    fun setReservaSeleccionada(reserva: ReservaHotel) {
        val previousSelected = reservaSeleccionada
        reservaSeleccionada = reserva

        // Actualizar las vistas
        reservas.indexOf(previousSelected).let { if (it != -1) notifyItemChanged(it) }
        reservas.indexOf(reserva).let { if (it != -1) notifyItemChanged(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reserva_hotel_seleccion, parent, false)
        return ReservaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        val reserva = reservas[position]
        holder.bind(reserva, reserva == reservaSeleccionada)
    }

    override fun getItemCount(): Int = reservas.size

    inner class ReservaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardReserva: CardView = itemView.findViewById(R.id.card_reserva)
        private val imgHotel: ImageView = itemView.findViewById(R.id.img_hotel_item)
        private val tvNombreHotel: TextView = itemView.findViewById(R.id.tv_nombre_hotel_item)
        private val tvDestino: TextView = itemView.findViewById(R.id.tv_destino_item)
        private val tvFechas: TextView = itemView.findViewById(R.id.tv_fechas_item)
        private val tvEstado: TextView = itemView.findViewById(R.id.tv_estado_item)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tv_precio_item)
        private val viewSeleccionado: View = itemView.findViewById(R.id.view_seleccionado)

        fun bind(reserva: ReservaHotel, isSelected: Boolean) {
            // Nombre del hotel
            tvNombreHotel.text = reserva.hotel?.nombre ?: "Hotel desconocido"

            // Destino
            tvDestino.text = reserva.destino

            // Fechas
            tvFechas.text = "${reserva.fechaEntrada} - ${reserva.fechaSalida}"

            // Estado
            tvEstado.text = reserva.estado
            when (reserva.estado) {
                "Confirmada" -> tvEstado.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                "Pendiente" -> tvEstado.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                else -> tvEstado.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
            }

            // Precio
            tvPrecio.text = "${reserva.precio.toInt()}€"

            // Imagen del hotel
            val imageUrl = reserva.hotel?.imagenUrl ?: ""
            if (imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.imagen_1)
                    .into(imgHotel)
            } else {
                imgHotel.setImageResource(R.drawable.imagen_1)
            }

            // Estado de selección
            if (isSelected) {
                cardReserva.setCardBackgroundColor(itemView.context.getColor(R.color.light_blue))
                viewSeleccionado.visibility = View.VISIBLE
            } else {
                cardReserva.setCardBackgroundColor(itemView.context.getColor(android.R.color.white))
                viewSeleccionado.visibility = View.GONE
            }

            // Click listener
            cardReserva.setOnClickListener {
                if (itemView.context is GuardarMonumentoActivity) {
                    (itemView.context as GuardarMonumentoActivity).onReservaSeleccionada(reserva)
                }
                onReservaSelected(reserva)
            }
        }
    }
}