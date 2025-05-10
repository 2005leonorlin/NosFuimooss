package com.example.nosfuimooss.model

data class DestinoInfo(
    val vuelo: Vuelo,
    val boletos: List<Boleto>,
    val hoteles: List<Hotel>
)