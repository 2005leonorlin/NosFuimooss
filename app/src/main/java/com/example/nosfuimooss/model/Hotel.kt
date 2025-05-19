package com.example.nosfuimooss.model

data class Hotel(
    val id: String,
    val nombre: String,
    val ubicacion: String,
    val precioNoche: Double,
    val disponible: Boolean,
    val imagenUrl: String,
    val estrellas: Int,
    val fechaDisponible: FechaDisponible,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val imagenes: List<String>
)

