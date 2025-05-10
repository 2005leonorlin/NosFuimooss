package com.example.nosfuimooss.api

import com.example.nosfuimooss.model.DestinoInfo
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface DestinoApiService {
    @GET("api/destinos/info-por-vuelo/{idVuelo}")
    fun getInfoPorIdVuelo(@Path("idVuelo") id: String): Call<DestinoInfo>
}