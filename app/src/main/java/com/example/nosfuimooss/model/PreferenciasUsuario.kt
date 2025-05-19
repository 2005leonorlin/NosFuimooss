package com.example.nosfuimooss.model

import java.io.Serializable

data class PreferenciasUsuario(
    val origen: String,
    val destino: String,
    val fechaInicio: Long,
    val fechaFin: Long,
    val cantidadPersonas: Int,
    val categoria: String
): Serializable
