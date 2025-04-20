package com.example.nosfuimooss.model

data class Destino(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val categoria: List<String> = emptyList(),
    val imagenes: List<String> = emptyList(),
    val bandera: String = "",
    val esFavorito: Boolean = false // Para manejar favoritos
)
