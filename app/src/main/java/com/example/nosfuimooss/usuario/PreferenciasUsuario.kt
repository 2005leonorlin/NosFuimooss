package com.example.nosfuimooss.usuario

import java.io.Serializable

data class PreferenciasUsuario(
    val origen: String,
    val destino: String,
    val fechaInicio: Long,
    val fechaFin: Long,
    val cantidadPersonas: Int,
    val categoria: String
): Serializable
