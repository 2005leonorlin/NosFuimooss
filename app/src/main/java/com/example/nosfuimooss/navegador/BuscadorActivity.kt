package com.example.nosfuimooss.navegador

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nosfuimooss.usuariologeado.DetalleVuelos
import com.example.NosFuimooss.R
import com.example.nosfuimooss.Adapter.DestinoAdapter
import com.example.nosfuimooss.model.Vuelo


class BuscadorActivity : AppCompatActivity() {
    private lateinit var searchInput: EditText
    private lateinit var searchResults: RecyclerView

    private lateinit var adapter: DestinoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscador)


        searchInput = findViewById(R.id.search_input)
        searchResults = findViewById(R.id.search_results)

        searchResults.layoutManager = LinearLayoutManager(this)
        adapter = DestinoAdapter(emptyList()) { destino ->
            val intent = Intent(this, DetalleVuelos::class.java)
            intent.putExtra("destinoId", destino.id)
            startActivity(intent)
        }
        searchResults.adapter = adapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(query: CharSequence?, start: Int, before: Int, count: Int) {
                buscarDestinos(query.toString().trim())
            }
        })
    }

    private fun buscarDestinos(texto: String) {
        if (texto.isEmpty()) return

        val call = com.example.nosfuimooss.api.RetrofitClient.vueloApiService.searchVuelosByNombre(texto)

        call.enqueue(object : retrofit2.Callback<List<Vuelo>> {
            override fun onResponse(
                call: retrofit2.Call<List<Vuelo>>,
                response: retrofit2.Response<List<Vuelo>>
            ) {
                if (response.isSuccessful) {
                    val destinos = response.body() ?: emptyList()
                    adapter.updateFavoritos(destinos, emptyList())
                } else {
                    Toast.makeText(this@BuscadorActivity, "Error en búsqueda", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<List<Vuelo>>, t: Throwable) {
                Toast.makeText(this@BuscadorActivity, "Error al conectar con la API", Toast.LENGTH_SHORT).show()
            }
        })
    }
}