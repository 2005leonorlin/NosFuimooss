package com.example.nosfuimooss.model

import java.io.Serializable

data class Pasajero(
    var nombre: String = "",
    var apellidos: String = "",
    var email: String = "",
    var dni: String = "",
    var edad: Int = 0,
    var genero: String = ""
) : Serializable