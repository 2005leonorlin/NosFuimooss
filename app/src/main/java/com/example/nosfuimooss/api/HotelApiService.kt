package com.example.nosfuimooss.api

import com.example.nosfuimooss.model.Hotel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface HotelApiService {
    @GET("api/hoteles")
    fun getAllHoteles(): Call<List<Hotel>>

    @GET("api/hoteles/{id}")
    fun getHotelById(@Path("id") id: String): Call<Hotel>

    @GET("api/hoteles/ubicacion/{ubicacion}")
    fun getHotelesByUbicacion(@Path("ubicacion") ubicacion: String): Call<List<Hotel>>
}