package com.example.nosfuimooss.api

import com.example.nosfuimooss.model.Boleto
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface BoletoApiService {

    @GET("api/boletos")
    fun getAllBoletos(): Call<List<Boleto>>

    @GET("api/boletos/por-destino/{destino}")
    fun getBoletosPorDestino(@Path("destino") destino: String): Call<List<Boleto>>
}