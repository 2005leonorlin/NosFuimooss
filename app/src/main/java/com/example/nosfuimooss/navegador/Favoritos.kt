package com.example.nosfuimooss.navegador

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.NosFuimooss.R
import com.example.nosfuimooss.Adapter.DestinoAdapter
import com.example.nosfuimooss.DetalleVuelos
import com.example.nosfuimooss.UsuarioLogeadoInicial
import com.example.nosfuimooss.model.Destino
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Favoritos : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var destinoAdapter: DestinoAdapter
    private var favoritosIds: MutableList<String> = mutableListOf()
    private var destinosFavoritos: List<Destino> = emptyList()
    private lateinit var recyclerDestinos: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_favoritos)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        recyclerDestinos = findViewById(R.id.recycler_favoritos)
        recyclerDestinos.layoutManager = GridLayoutManager(this, 2)

        destinoAdapter = DestinoAdapter(
            destinosFavoritos,
            favoritosIds,
            onItemClick = { destino ->
                val intent = Intent(this, DetalleVuelos::class.java)
                intent.putExtra("destinoId", destino.id)
                startActivity(intent)
            },
            onFavoriteClick = { destino ->
                toggleFavoriteInFavoritos(destino)
            }
        )
        recyclerDestinos.adapter = destinoAdapter

        loadUserInfo()
        fetchFavoritos()
        setupNavigation()
    }

    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userNameText = findViewById<TextView>(R.id.user_name_text)
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().reference
                .child("users")
                .child(userId)

            database.get().addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(com.example.nosfuimooss.model.User::class.java)
                if (user != null) {
                    userNameText.text = " ${user.name} "
                } else {
                    userNameText.text = "Usuario"
                }
            }.addOnFailureListener {
                userNameText.text = "Usuario"
            }
        }
    }

    private fun fetchFavoritos() {
        val userId = auth.currentUser?.uid ?: return

        // Fetch favorite IDs
        firestore.collection("users").document(userId)
            .collection("favorites")
            .get()
            .addOnSuccessListener { result ->
                favoritosIds.clear()
                val favoriteIds = result.documents.map { it.id }
                favoritosIds.addAll(favoriteIds)

                if (favoritosIds.isEmpty()) {
                    // Show empty state or message
                    Toast.makeText(this, "No tienes favoritos guardados", Toast.LENGTH_SHORT).show()
                } else {
                    // Fetch destination details for each favorite
                    fetchDestinosFavoritos(favoritosIds)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar favoritos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchDestinosFavoritos(ids: List<String>) {
        val destinosTemp = mutableListOf<Destino>()
        var loadedCount = 0

        ids.forEach { id ->
            firestore.collection("Viajes").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val destino = Destino(
                            id = document.id,
                            nombre = document.getString("nombre") ?: "",
                            descripcion = document.getString("descripcion") ?: "",
                            categoria = document.get("categoria") as? List<String> ?: emptyList(),
                            imagenes = (document.get("imagenes") as? List<String>) ?: emptyList(),
                            bandera = document.getString("bandera") ?: ""
                        )
                        destinosTemp.add(destino)
                    }

                    loadedCount++
                    if (loadedCount == ids.size) {
                        // All destinations loaded
                        destinosFavoritos = destinosTemp
                        destinoAdapter.updateFavoritos(destinosFavoritos, favoritosIds)
                    }
                }
                .addOnFailureListener { e ->
                    loadedCount++
                    Toast.makeText(this, "Error al cargar destino: ${e.message}", Toast.LENGTH_SHORT).show()

                    if (loadedCount == ids.size) {
                        // All destinations attempted
                        destinosFavoritos = destinosTemp
                        destinoAdapter.updateFavoritos(destinosFavoritos, favoritosIds)
                    }
                }
        }
    }

    private fun toggleFavoriteInFavoritos(destino: Destino) {
        val userId = auth.currentUser?.uid ?: return
        val favoritoRef = firestore.collection("users").document(userId).collection("favorites").document(destino.id)

        if (favoritosIds.contains(destino.id)) {

            favoritoRef.delete()
            favoritosIds.remove(destino.id)
        } else {

            favoritoRef.set(destino)
            favoritosIds.add(destino.id)
        }


        destinoAdapter.toggleFavorito(destino.id)


        destinoAdapter.updateFavoritos(destinosFavoritos, favoritosIds)
    }

    private fun setupNavigation() {
        // Setup bottom navigation
        findViewById<ImageView>(R.id.nav_home).setOnClickListener {
            startActivity(Intent(this, UsuarioLogeadoInicial::class.java))
            finish()
        }


    }


    override fun onResume() {
        super.onResume()
        fetchFavoritos()
    }
}