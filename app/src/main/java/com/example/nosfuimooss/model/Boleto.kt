package com.example.nosfuimooss.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Boleto(
    val id: String = "",
    val origen: String = "",
    val destino: String = "",
    val precio: Double = 0.0,
    @SerializedName("fecha_inicial")
    val fechaInicial: String = "",
    @SerializedName("fecha_final")
    val fechaFinal: String = "",
    val duracion: String = ""



): Serializable
