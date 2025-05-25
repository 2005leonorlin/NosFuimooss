package com.example.nosfuimooss.usuario

import com.example.nosfuimooss.model.Boleto
import java.io.Serializable

data class Reserva(
    var id: String = "",
    val userId: String = "",
    // Campos principales de fechas
    val fechaIda: Long = 0L,
    val fechaVuelta: Long = 0L,
    val fechaReserva: Long = 0L,

    val boletoIda: Boleto? = null,
    val boletoVuelta: Boleto? = null,

    // Horarios
    val idaDepartureTime: String = "",
    val idaArrivalTime: String = "",
    val vueltaDepartureTime: String = "",
    val vueltaArrivalTime: String = "",
    val horaSalida: String = "", // Campo adicional encontrado en Firebase

    // Información del viaje
    val origen: String = "",
    val destino: String = "",
    val duracion: String = "",
    val categoria: String = "",
    val banderaUrl: String = "",
    val imagenDestinoUrl: String = "",

    // Información del pasajero
    val nombrePasajero: String = "",
    val numPasajeros: Int = 0,
    val pasajeros: List<Pasajero> = emptyList(),

    // Información de la reserva
    val precio: Double = 0.0,
    val roundTrip: Boolean = false,
    val estado: String = "Pendiente", // Pendiente, Confirmada, Cancelada, Completada
    val tarifa: String = "",
    val clase: String = "",
    val imageUrl: String = ""
) : Serializable