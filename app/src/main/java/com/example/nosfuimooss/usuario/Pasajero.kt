package com.example.nosfuimooss.usuario

import java.io.Serializable

data class Pasajero(
    var nombre: String = "",
    var apellidos: String = "",
    var email: String = "",
    var dni: String = "",
    var edad: Int = 0,
    var genero: String = "",

    var telefono: String = "",
    var tipoDocumento: String = "DNI",
    var numeroDocumento: String = ""
) : Serializable
