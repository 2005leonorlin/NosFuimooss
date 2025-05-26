package com.example.nosfuimooss.usuario

import java.io.Serializable

data class ReservaActividad(
    val id: String = "",
    val userId: String = "",
    val actividadId: String = "",
    val nombreActividad: String = "",
    val ubicacion: String = "",
    val fechaActividad: String = "",
    val numeroPasajeros: Int = 1,
    val precioTotal: Double = 0.0,
    val precioBase: Double = 0.0,
    val fechaReserva: Long = 0L,
    val estado: String = "Pendiente", // "Confirmada", "Cancelada", "Completada"
    val duracion: String = "",
    val horario: String = "",
    val imageUrl: String = "",
    val puntoRecogida: String = "",
    val queEstaIncluido: String = ""
) : Serializable