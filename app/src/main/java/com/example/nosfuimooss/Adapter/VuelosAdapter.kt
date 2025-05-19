package com.example.nosfuimooss.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Boleto
import com.example.nosfuimooss.model.Vuelo

class VuelosAdapter(
    private var boletos: List<Boleto>,
    private var vuelos: List<Vuelo>,
    private val onClick: (Boleto) -> Unit
) : RecyclerView.Adapter<VuelosAdapter.VueloViewHolder>() {

    inner class VueloViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtRuta: TextView = itemView.findViewById(R.id.txtRuta)
        val txtPrecio: TextView = itemView.findViewById(R.id.txtPrecio)
        val txtDuracion: TextView = itemView.findViewById(R.id.txtDuracion)
        val btnReservar: Button = itemView.findViewById(R.id.btnReservar)
        val imgDestino: ImageView = itemView.findViewById(R.id.imgDestino)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VueloViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vuelo_disponible, parent, false)
        return VueloViewHolder(view)
    }

    override fun getItemCount(): Int = boletos.size

    override fun onBindViewHolder(holder: VueloViewHolder, position: Int) {
        val boleto = boletos[position]

        holder.txtRuta.text = "${boleto.origen} → ${boleto.destino}"
        holder.txtPrecio.text = "Precio vuelo: ${boleto.precio}€"
        holder.txtDuracion.text = "Duración: ${boleto.duracion}"

        // Buscar imagen del destino
        val vueloCoincidente = vuelos.find { it.nombre.equals(boleto.destino, ignoreCase = true) }
        vueloCoincidente?.principal?.let { url ->
            // Usa tu librería de imágenes preferida (por ejemplo Glide o Picasso)
            Glide.with(holder.itemView.context)
                .load(url)

                .into(holder.imgDestino)
        }

        holder.btnReservar.setOnClickListener { onClick(boleto) }
    }
    fun updateVuelos(nuevosVuelos: List<Vuelo>) {
        this.vuelos = nuevosVuelos
        notifyDataSetChanged()  // para que onBind vuelva a ejecutarse
    }

    fun updateData(nuevosBoletos: List<Boleto>) {
        boletos = nuevosBoletos
        notifyDataSetChanged()
    }
}