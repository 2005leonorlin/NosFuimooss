package com.example.nosfuimooss.model

data class Vuelo(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val categoria: List<String> = emptyList(),
    val imagenes: List<String> = emptyList(),
    val bandera: String = "",
    val principal: String = "",
    val esFavorito: Boolean = false // Para manejar favoritos
)
