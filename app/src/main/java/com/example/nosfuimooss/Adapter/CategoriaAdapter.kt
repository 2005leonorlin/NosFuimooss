package com.example.nosfuimooss.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Categoria


class CategoriaAdapter(
    private val categorias: List<Categoria>,
    private val onItemClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {

    // Índice seleccionado actualmente
    private var selectedPosition = 0

    // Exponer la categoría actualmente seleccionada
    fun getCategoriaSeleccionada(): Categoria = categorias[selectedPosition]

    inner class CategoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreCategoria: TextView = itemView.findViewById(R.id.nombre_categoria)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && position != selectedPosition) {
                    val oldSelected = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(oldSelected)
                    notifyItemChanged(selectedPosition)
                    onItemClick(categorias[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_categoria, parent, false)
        return CategoriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        val categoria = categorias[position]
        holder.nombreCategoria.text = categoria.nombre

        // Estilo según selección
        if (position == selectedPosition) {
            holder.itemView.setBackgroundResource(R.color.colorAccent)
            holder.nombreCategoria.setTextColor(
                holder.itemView.context.getColor(android.R.color.white)
            )
        } else {
            holder.itemView.setBackgroundResource(android.R.color.white)
            holder.nombreCategoria.setTextColor(
                holder.itemView.context.getColor(android.R.color.black)
            )
        }
    }

    override fun getItemCount() = categorias.size
}
