package com.example.nosfuimooss.model

import java.io.Serializable

data class Reserva(
    val id: String = "",
    val userId: String = "",

    val boletoIda: Boleto? = null,
    val boletoVuelta: Boleto? = null,
    val fechaIda: Long = 0L,
    val fechaVuelta: Long = 0L,
    val idaDepartureTime: String = "",
    val idaArrivalTime: String = "",
    val vueltaDepartureTime: String = "",
    val vueltaArrivalTime: String = "",
    val pasajeros: List<Pasajero> = emptyList(),
    val roundTrip: Boolean = false,
    val precio: Double = 0.0,
    val fechaReserva: Long = 0L,
    val estado: String = "Pendiente", // Pendiente, Confirmada, Cancelada, Completada
    val tarifa: String = "",
    val clase: String = "",
    val imageUrl: String = ""
) : Serializable