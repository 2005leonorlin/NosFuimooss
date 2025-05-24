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
import com.example.nosfuimooss.usuariologeado.DetalleVuelos
import com.example.nosfuimooss.usuariologeado.UsuarioLogeadoInicial
import com.example.nosfuimooss.api.RetrofitClient
import com.example.nosfuimooss.meFaltaHacer.UsuarioPerfil
import com.example.nosfuimooss.model.Vuelo
import com.example.nosfuimooss.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Favoritos : AppCompatActivity() {

    private lateinit var recyclerDestinos: RecyclerView
    private lateinit var destinoAdapter: DestinoAdapter

    // Lista en memoria de IDs y de objetos Destino
    private val favoritosIds = mutableListOf<String>()
    private var destinosFavoritos: List<Vuelo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_favoritos)

        recyclerDestinos = findViewById(R.id.recycler_favoritos)
        recyclerDestinos.layoutManager = GridLayoutManager(this, 2)

        // Inicializa el adapter con listas vacías
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
        loadUserData()
        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        cargarFavoritos()  // carga desde Firebase y dentro del success llama a fetchDestinosFavoritos
    }
    private fun loadUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userNameText = findViewById<TextView>(R.id.user_name_text)

            FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                userNameText.text = if (user?.name?.isNotBlank() == true)
                    "${user.name}"
                else
                    "Hola, usuario"
            }
            .addOnFailureListener {
                userNameText.text = "Hola, usuario"
            }
    }


    private fun cargarFavoritos() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(userId)
            .child("favorites")

        ref.get().addOnSuccessListener { snap ->
            favoritosIds.clear()
            snap.children.forEach { favoritosIds.add(it.key!!) }

            if (favoritosIds.isEmpty()) {
                Toast.makeText(this, "No tienes favoritos guardados", Toast.LENGTH_SHORT).show()
                destinoAdapter.updateFavoritos(emptyList(), favoritosIds)
            } else {
                fetchDestinosFavoritos(favoritosIds)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar favoritos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchDestinosFavoritos(ids: List<String>) {
        val temp = mutableListOf<Vuelo>()
        var loaded = 0

        ids.forEach { id ->
            RetrofitClient.vueloApiService.getVueloById(id)
                .enqueue(object : Callback<Vuelo> {
                    override fun onResponse(call: Call<Vuelo>, resp: Response<Vuelo>) {
                        resp.body()?.let { temp.add(it) }
                        loaded++
                        if (loaded == ids.size) {
                            destinosFavoritos = temp
                            destinoAdapter.updateFavoritos(destinosFavoritos, favoritosIds)
                        }
                    }
                    override fun onFailure(call: Call<Vuelo>, t: Throwable) {
                        loaded++
                        if (loaded == ids.size) {
                            destinosFavoritos = temp
                            destinoAdapter.updateFavoritos(destinosFavoritos, favoritosIds)
                        }
                    }
                })
        }
    }

    private fun toggleFavoriteInFavoritos(vuelo: Vuelo) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance()
            .reference
            .child("users")
            .child(userId)
            .child("favorites")
            .child(vuelo.id)

        if (favoritosIds.contains(vuelo.id)) {
            ref.removeValue().addOnSuccessListener {
                Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                cargarFavoritos()
            }
        } else {
            ref.setValue(true).addOnSuccessListener {
                Toast.makeText(this, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
                cargarFavoritos()
            }
        }
    }

    private fun setupNavigation() {

        findViewById<ImageView>(R.id.nav_home).setOnClickListener {
            Toast.makeText(this, "Destinos", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, UsuarioLogeadoInicial::class.java))
        }
        findViewById<ImageView>(R.id.nav_flight).setOnClickListener {
            Toast.makeText(this, "Búsqueda de vuelos", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ElegirVuelo::class.java))
        }
        findViewById<ImageView>(R.id.nav_moon).setOnClickListener {
            Toast.makeText(this, "Mis Viajes", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MisViajesActivity::class.java))
        }
        findViewById<ImageView>(R.id.nav_heart).setOnClickListener {
            Toast.makeText(this, "Favoritos", Toast.LENGTH_SHORT).show()

        }
        findViewById<ImageView>(R.id.nav_profile).setOnClickListener {
            Toast.makeText(this, "Perfil", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, UsuarioPerfil::class.java))
        }
        findViewById<ImageView>(R.id.ic_calendar).setOnClickListener {
            startActivity(Intent(this, Calendario::class.java))
        }
    }
}