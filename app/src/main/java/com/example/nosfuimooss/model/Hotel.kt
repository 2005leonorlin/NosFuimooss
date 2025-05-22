package com.example.nosfuimooss.model

import java.io.Serializable

data class Hotel(
    val id: String,
    val nombre: String,
    val ubicacion: String,
    val precioNoche: Double,
    val disponible: Boolean,
    val imagenUrl: String,
    val estrellas: Int,
    val fechaDisponible: FechaDisponible? = null,
    val direccion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val imagenes: List<String>? = null
): Serializable {

    // Constructor sin argumentos para Firebase
    constructor() : this(
        id = "",
        nombre = "",
        ubicacion = "",
        precioNoche = 0.0,
        disponible = false,
        imagenUrl = "",
        estrellas = 0
    )
}

