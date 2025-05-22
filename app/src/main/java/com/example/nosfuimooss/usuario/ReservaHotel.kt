package com.example.nosfuimooss.usuario

import com.example.nosfuimooss.model.Hotel
import java.io.Serializable

data class ReservaHotel(
    val id: String = "",
    val userId: String = "",
    val hotel: Hotel? = null,
    val fechaEntrada: String = "",
    val fechaSalida: String = "",
    val adultos: Int = 1,
    val ninos: Int = 0,
    val tipoHabitacion: String = "",
    val noches: Int = 1,
    val precio: Double = 0.0,
    val fechaReserva: Long = 0L,
    var estado: String = "Pendiente", // Confirmada, Cancelada, Completada
    val destino: String = ""
) : Serializable