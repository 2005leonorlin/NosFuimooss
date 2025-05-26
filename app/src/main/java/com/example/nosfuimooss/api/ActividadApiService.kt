package com.example.nosfuimooss.api

import com.example.nosfuimooss.model.Actividad
import retrofit2.Call
import retrofit2.http.GET

import retrofit2.http.Path

interface ActividadApiService {
    @GET("api/actividades")
    fun getAllActividades(): Call<List<Actividad>>

    @GET("api/actividades/{id}")
    fun getActividadById(@Path("id") id: String): Call<Actividad>


}