package com.example.nosfuimooss.model

data class Hotel(
    val id: String,
    val nombre: String,
    val ubicacion: String,
    val precioNoche: Double,
    val disponible: Boolean,
    val imagen: String,
    val estrellas: String,
    val fechaDisponible: FechaDisponible? = null
)

