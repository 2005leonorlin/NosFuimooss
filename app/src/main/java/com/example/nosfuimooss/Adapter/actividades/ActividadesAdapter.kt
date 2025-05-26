package com.example.nosfuimooss.Adapter.actividades

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Actividad
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ActividadesAdapter(
    private val actividades: List<Actividad>,
    private val onActivityClick: (Actividad) -> Unit
) : RecyclerView.Adapter<ActividadesAdapter.ActividadViewHolder>() {

    class ActividadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val activityImage: ImageView = itemView.findViewById(R.id.activity_image)
        val activityTitle: TextView = itemView.findViewById(R.id.activity_title)
        val activitySubtitle: TextView = itemView.findViewById(R.id.activity_subtitle)
        val activityPrice: TextView = itemView.findViewById(R.id.activity_price)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.favorite_icon)
        val detailsButton: Button = itemView.findViewById(R.id.details_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActividadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_card, parent, false)
        return ActividadViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActividadesAdapter.ActividadViewHolder, position: Int) {
        val actividad = actividades[position]

        // Configurar título
        holder.activityTitle.text = actividad.nombre ?: "Actividad sin nombre"

        // Configurar subtítulo (ubicación y duración)
        val subtitle = buildString {
            actividad.ubicacion?.let { append(it) }
            actividad.tiempo?.let {
                if (isNotEmpty()) append(" - ")
                append(it)
            }
        }
        holder.activitySubtitle.text = subtitle.ifEmpty { "Sin información adicional" }

        // Configurar precio - CORREGIDO para manejar Double
        holder.activityPrice.text = if (actividad.precio > 0) {
            formatPrice(actividad.precio)
        } else {
            "Precio no disponible"
        }

        // Cargar imagen
        if (!actividad.foto.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(actividad.foto)
                .centerCrop()
                .placeholder(R.drawable.imagen_3) // imagen por defecto
                .error(R.drawable.imagen_3) // imagen en caso de error
                .into(holder.activityImage)
        } else {
            holder.activityImage.setImageResource(R.drawable.imagen_3)
        }

        // Configurar botón de favoritos
        setupFavoriteButton(holder, actividad)

        // Click en el botón de detalles
        holder.detailsButton.setOnClickListener {
            onActivityClick(actividad)
        }

        // Click en toda la tarjeta
        holder.itemView.setOnClickListener {
            onActivityClick(actividad)
        }
    }

    private fun formatPrice(price: Double): String {
        return String.format("%.2f €", price)
    }

    private fun setupFavoriteButton(holder: ActividadViewHolder, actividad: Actividad) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            holder.favoriteIcon.setImageResource(R.drawable.ic_megusta)
            return
        }

        val actividadId = actividad.id ?: return

        // CORREGIDO: Usar el mismo nombre de nodo que en DetalleActividadActivity
        val ref = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("favorites_activities") // Cambiado de "favoritesActividades" a "favorites_activities"
            .child(actividadId)

        // Verificar si es favorito
        ref.get().addOnSuccessListener { snapshot ->
            val isFavorite = snapshot.exists()
            holder.favoriteIcon.setImageResource(
                if (isFavorite) R.drawable.ic_corazon_completo
                else R.drawable.ic_megusta
            )
        }

        // Click en favoritos
        holder.favoriteIcon.setOnClickListener {
            toggleFavorite(userId, actividad, holder.favoriteIcon)
        }
    }

    private fun toggleFavorite(userId: String, actividad: Actividad, favoriteIcon: ImageView) {
        val actividadId = actividad.id ?: return

        val ref = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("favorites_activities") // Usar el mismo nombre de nodo
            .child(actividadId)

        ref.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Remover de favoritos
                ref.removeValue().addOnSuccessListener {
                    favoriteIcon.setImageResource(R.drawable.ic_megusta)
                }
            } else {
                // Agregar a favoritos - CORREGIDO: Guardar los mismos datos que en DetalleActividadActivity
                val favoriteData = mapOf(
                    "id" to actividadId,
                    "nombre" to (actividad.nombre ?: ""),
                    "ubicacion" to (actividad.ubicacion ?: ""),
                    "precio" to actividad.precio, // Guardamos el precio como Double
                    "foto" to (actividad.foto ?: ""),
                    "timestamp" to System.currentTimeMillis()
                )

                ref.setValue(favoriteData).addOnSuccessListener {
                    favoriteIcon.setImageResource(R.drawable.ic_corazon_completo)
                }
            }
        }
    }

    override fun getItemCount(): Int = actividades.size
}