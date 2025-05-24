package com.example.nosfuimooss.model

import java.io.Serializable

data class Monumento(
    var id: String? = null,
    var nombre: String? = null,
    var informacion: String? = null,
    var foto: String? = null,
    var ubicacion: String? = null,
    var estrella: Double? = null,
    var latitud: Double? = null,
    var longitud: Double? = null,
    var direccion: String? = null
): Serializable