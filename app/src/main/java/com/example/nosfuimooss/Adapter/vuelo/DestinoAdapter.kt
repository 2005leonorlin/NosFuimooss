package com.example.nosfuimooss.Adapter.vuelo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Vuelo


class DestinoAdapter(
    private var vuelos: List<Vuelo>,
    private var favoritosIds: List<String> = emptyList(),
    private val onItemClick: (Vuelo) -> Unit = {},
    private val onFavoriteClick: (Vuelo) -> Unit = {}

) : RecyclerView.Adapter<DestinoAdapter.DestinoViewHolder>() {

    inner class DestinoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreDestino: TextView = itemView.findViewById(R.id.nombre_destino)
        val imagenDestino: ImageView = itemView.findViewById(R.id.imagen_destino)
        val banderaDestino: ImageView = itemView.findViewById(R.id.bandera_destino)
        val favoriteButton: ImageView = itemView.findViewById(R.id.ic_favorite)

        init {

            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Siempre llamamos a onItemClick, y dejamos que la actividad
                    // se encargue de verificar si el usuario está autenticado
                    onItemClick(vuelos[position])
                }
            }

            // Al hacer clic en el botón de favorito
            favoriteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFavoriteClick(vuelos[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestinoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_destino, parent, false)
        return DestinoViewHolder(view)

    }

    override fun onBindViewHolder(holder: DestinoViewHolder, position: Int) {
        val destino = vuelos[position]

        if (favoritosIds.contains(destino.id)) {
            holder.favoriteButton.setImageResource(R.drawable.ic_corazon_completo)
        } else {
            holder.favoriteButton.setImageResource(R.drawable.ic_megusta)
        }

        holder.nombreDestino.text = destino.nombre
        // Cargar imagen principal
        Glide.with(holder.itemView.context)
            .load(destino.imagenes.firstOrNull()) // Carga la primera imagen
            .centerCrop()
            .placeholder(R.drawable.placeholder_image)
            .into(holder.imagenDestino)

        // Cargar bandera
        Glide.with(holder.itemView.context)
            .load(destino.bandera)
            .into(holder.banderaDestino)


    }

    override fun getItemCount() = vuelos.size

    fun updateFavoritos(newVuelos: List<Vuelo>, newFavoritos: List<String>) {
        vuelos = newVuelos
        favoritosIds = newFavoritos.toMutableList()
        notifyDataSetChanged()
    }
    fun toggleFavorito(destinoId: String) {
        val position = vuelos.indexOfFirst { it.id == destinoId }
        if (position != -1) {
            if (favoritosIds.contains(destinoId)) {
                favoritosIds = favoritosIds - destinoId
            } else {
                favoritosIds = favoritosIds + destinoId
            }

            notifyItemChanged(position)
        }
    }
}