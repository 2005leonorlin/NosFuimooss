package com.example.nosfuimooss.api

import com.example.nosfuimooss.model.Monumento
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MonumentoApiService {
    @GET("api/monumentos")
    fun getAllMonumentos(): Call<List<Monumento>>

    @GET("api/monumentos/ubicacion/{ubicacion}")
    fun getMonumentosByUbicacion(@Path("ubicacion") ubicacion: String): Call<List<Monumento>>

    @GET("api/monumentos/ubicacion")
    fun getMonumentosByUbicacionQuery(@Query("ubicacion") ubicacion: String): Call<List<Monumento>>

    @GET("api/monumentos/{id}")
    fun getMonumentoById(@Path("id") id: String): Call<Monumento>
}