package com.example.nosfuimooss.meFaltaHacer

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nosfuimooss.DetalleVuelos
import com.example.NosFuimooss.R
import com.example.nosfuimooss.Adapter.DestinoAdapter
import com.example.nosfuimooss.model.Destino
import com.google.firebase.firestore.FirebaseFirestore

class BuscadorActivity : AppCompatActivity() {
    private lateinit var searchInput: EditText
    private lateinit var searchResults: RecyclerView
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: DestinoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buscador)

        db = FirebaseFirestore.getInstance()
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

        db.collection("Viajes")
            .orderBy("nombre")
            .startAt(texto)
            .endAt(texto + "\uf8ff")
            .get()
            .addOnSuccessListener { result ->
                val destinos = result.map { doc ->
                    Destino(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        categoria = doc.get("categoria") as? List<String> ?: listOf(),
                        imagenes = doc.get("imagenes") as? List<String> ?: listOf(),
                        bandera = doc.getString("bandera") ?: ""
                    )
                }
                adapter.updateFavoritos(
                    destinos,emptyList()

                )
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al buscar", Toast.LENGTH_SHORT).show()
            }
    }
}