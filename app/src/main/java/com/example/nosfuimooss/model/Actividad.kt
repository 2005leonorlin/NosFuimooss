package com.example.nosfuimooss.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Actividad(
    val id: String? = null,
    val nombre: String? = null,
    val ubicacion: String? = null,
    val estrellas: Double = 0.0,
    val precio: Double = 0.0,
    @SerializedName("fecha_inicio")
    val fechaInicio: String? = null,
    @SerializedName("fecha_final")
    val fechaFinal: String? = null,
    val tiempo: String? = null,
    val longitud: Double = 0.0,
    val latitud: Double = 0.0,
    @SerializedName("resumen_del_viaje")
    val resumen: String? = null,
    @SerializedName("que_esta_incluido")
    val queEstaIncluido: String? = null,
    @SerializedName("punto_de_recogida")
    val puntoRecogida: String? = null,
    val foto: String? = null
) : Serializable