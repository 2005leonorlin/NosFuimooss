package com.example.nosfuimooss.api

import com.example.nosfuimooss.model.Hotel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface HotelApiService {
    @GET("api/hoteles")
    fun getAllHoteles(): Call<List<Hotel>>

    @GET("api/hoteles/destino/{ubicacion}")
    fun getHotelesPorUbicacion(@Path("ubicacion") ubicacion: String): Call<List<Hotel>>
}