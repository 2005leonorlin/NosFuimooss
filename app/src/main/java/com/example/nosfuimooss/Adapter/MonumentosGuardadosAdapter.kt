package com.example.nosfuimooss.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Monumento

class MonumentosGuardadosAdapter(
    private val monumentos: List<Monumento>,
    private val onMonumentoClick: (Monumento) -> Unit
) : RecyclerView.Adapter<MonumentosGuardadosAdapter.MonumentoViewHolder>() {

    inner class MonumentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgMonumento: ImageView = itemView.findViewById(R.id.img_monumento)
        private val tvNombreMonumento: TextView = itemView.findViewById(R.id.tv_nombre_monumento)
        private val tvUbicacionMonumento: TextView = itemView.findViewById(R.id.tv_ubicacion_monumento)

        fun bind(monumento: Monumento) {
            // Configurar nombre del monumento
            tvNombreMonumento.text = monumento.nombre ?: "Monumento desconocido"

            // Configurar ubicación
            tvUbicacionMonumento.text = monumento.ubicacion ?: "Ubicación no disponible"

            // Cargar imagen del monumento
            val imageUrl = monumento.foto
            if (!imageUrl.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.imagen_1)
                    .error(R.drawable.imagen_1)
                    .centerCrop()
                    .into(imgMonumento)
            } else {
                imgMonumento.setImageResource(R.drawable.imagen_1)
            }

            // Configurar click listener
            itemView.setOnClickListener {
                onMonumentoClick(monumento)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonumentoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monumento_guardado, parent, false)
        return MonumentoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonumentoViewHolder, position: Int) {
        holder.bind(monumentos[position])
    }

    override fun getItemCount(): Int = monumentos.size
}
