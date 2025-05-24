package com.example.nosfuimooss.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Monumento

class MonumentosAdapter(
    private val monumentos: List<Monumento>,
    private val onItemClick: (Monumento) -> Unit
) : RecyclerView.Adapter<MonumentosAdapter.MonumentoViewHolder>() {

    class MonumentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val monumentImage: ImageView = itemView.findViewById(R.id.monument_image)
        val monumentTitle: TextView = itemView.findViewById(R.id.monument_title)
        val monumentDescription: TextView = itemView.findViewById(R.id.monument_description)
        val ratingText: TextView = itemView.findViewById(R.id.rating_text)
        val starImages: List<ImageView> = listOf(
            itemView.findViewById(R.id.star_1),
            itemView.findViewById(R.id.star_2),
            itemView.findViewById(R.id.star_3),
            itemView.findViewById(R.id.star_4),
            itemView.findViewById(R.id.star_5)
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonumentoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monument_card, parent, false)
        return MonumentoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonumentoViewHolder, position: Int) {
        val monumento = monumentos[position]

        // Cargar imagen del monumento
        Glide.with(holder.itemView.context)
            .load(monumento.foto)
            .placeholder(R.drawable.imagen_1) // Imagen por defecto mientras carga
            .error(R.drawable.imagen_1) // Imagen por defecto si hay error
            .centerCrop()
            .into(holder.monumentImage)

        // Configurar textos
        holder.monumentTitle.text = monumento.nombre?.uppercase() ?: "SIN NOMBRE"
        holder.monumentDescription.text = monumento.informacion ?: "Sin información disponible"

        // Configurar rating
        val rating = monumento.estrella ?: 0.0
        holder.ratingText.text = rating.toString()

        // Configurar estrellas basadas en el rating
        configureStars(holder.starImages, rating, holder.itemView.context)

        // Configurar click listener para navegar al detalle
        holder.itemView.setOnClickListener {
            onItemClick(monumento)
        }
    }

    override fun getItemCount(): Int = monumentos.size

    private fun configureStars(starImages: List<ImageView>, rating: Double, context: android.content.Context) {
        val fullStars = rating.toInt()
        val hasHalfStar = rating - fullStars >= 0.5

        for (i in starImages.indices) {
            when {
                i < fullStars -> {
                    // Estrella completa
                    starImages[i].setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_star)
                    )
                    starImages[i].imageTintList = ContextCompat.getColorStateList(context, android.R.color.holo_orange_light)
                }
                i == fullStars && hasHalfStar -> {
                    // Media estrella
                    starImages[i].setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_star)
                    )
                    starImages[i].imageTintList = ContextCompat.getColorStateList(context, android.R.color.holo_orange_light)
                }
                else -> {
                    // Estrella vacía
                    starImages[i].setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_star)
                    )
                    starImages[i].imageTintList = ContextCompat.getColorStateList(context, android.R.color.darker_gray)
                }
            }
        }
    }
}