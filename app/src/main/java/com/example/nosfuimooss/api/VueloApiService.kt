package com.example.nosfuimooss.api

import com.example.nosfuimooss.model.Vuelo
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
interface VueloApiService {
    @GET("api/vuelos")
    fun getAllVuelos(): Call<List<Vuelo>>

    @GET("api/vuelos/categoria/{categoria}")
    fun getVuelosByCategoria(@Path("categoria") categoria: String): Call<List<Vuelo>>

    @GET("api/vuelos/buscar")
    fun searchVuelosByNombre(@Query("nombre") nombre: String): Call<List<Vuelo>>

    @GET("api/vuelos/{id}")
    fun getVueloById(@Path("id") id: String): Call<Vuelo>
}
