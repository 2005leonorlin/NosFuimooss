package com.example.nosfuimooss.usuariologeado

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.NosFuimooss.R
import com.example.nosfuimooss.Adapter.CategoriaAdapter
import com.example.nosfuimooss.Adapter.DestinoAdapter
import com.example.nosfuimooss.api.RetrofitClient
import com.example.nosfuimooss.meFaltaHacer.Calendario
import com.example.nosfuimooss.meFaltaHacer.ElegirVuelo
import com.example.nosfuimooss.navegador.Favoritos
import com.example.nosfuimooss.navegador.MisViajesActivity
import com.example.nosfuimooss.meFaltaHacer.UsuarioPerfil
import com.example.nosfuimooss.model.Categoria
import com.example.nosfuimooss.model.Vuelo
import com.example.nosfuimooss.model.User
import com.example.nosfuimooss.sesion.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UsuarioLogeadoInicial : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var categoriaAdapter: CategoriaAdapter
    private lateinit var destinoAdapter: DestinoAdapter
    private var favoritosIds: MutableList<String> = mutableListOf()
    private var destinosActuales: List<Vuelo> = emptyList()
    private lateinit var recyclerCategorias: RecyclerView
    private lateinit var recyclerDestinos: RecyclerView

    private val categoriasFijas = listOf(
        Categoria("Popular"),
        Categoria("Playa"),
        Categoria("Naturaleza"),
        Categoria("Urbano"),
        Categoria("Cultural"),
        Categoria("Exótico")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_usuario_logeado_inicial)

        auth = FirebaseAuth.getInstance()
        fetchUserData()

        recyclerCategorias = findViewById(R.id.recycler_categorias)
        recyclerDestinos = findViewById(R.id.recycler_destinos)

        recyclerCategorias.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerDestinos.layoutManager = GridLayoutManager(this, 2)

        val searchInput = findViewById<android.widget.EditText>(R.id.search_input)
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val texto = s.toString().trim()
                if (texto.isNotEmpty()) {
                    buscarDestinosPorNombre(texto)
                } else {
                    fetchDestinosFiltrados(categoriasFijas[0].nombre)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        categoriaAdapter = CategoriaAdapter(categoriasFijas) { categoria ->
            fetchDestinosFiltrados(categoria.nombre)
        }
        recyclerCategorias.adapter = categoriaAdapter

        destinoAdapter = DestinoAdapter(
            destinosActuales,
            favoritosIds,
            onItemClick = { destino ->
                if (auth.currentUser != null) {
                    val intent = Intent(this, DetalleVuelos::class.java)
                    intent.putExtra("destinoId", destino.id)
                    startActivity(intent)
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            },
            onFavoriteClick = { destino ->
                toggleFavorite(destino)
            }
        )
        recyclerDestinos.adapter = destinoAdapter

        fetchDestinosFiltrados(categoriasFijas[0].nombre)

        findViewById<ImageView>(R.id.nav_home).setOnClickListener {
            startActivity(Intent(this, UsuarioLogeadoInicial::class.java))
        }

        findViewById<ImageView>(R.id.nav_flight).setOnClickListener {
            startActivity(Intent(this, ElegirVuelo::class.java))
        }

        findViewById<ImageView>(R.id.nav_moon).setOnClickListener {
            startActivity(Intent(this, MisViajesActivity::class.java))
        }

        findViewById<ImageView>(R.id.nav_heart).setOnClickListener {
            startActivity(Intent(this, Favoritos::class.java))
        }

        findViewById<ImageView>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, UsuarioPerfil::class.java))
        }

        findViewById<ImageView>(R.id.ic_calendar).setOnClickListener {
            startActivity(Intent(this, Calendario::class.java))
        }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid
        val userNameText = findViewById<TextView>(R.id.user_name_text)

        if (userId != null) {
            val database = FirebaseDatabase.getInstance().reference.child("users").child(userId)
            database.get().addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    userNameText.text = "Hola, ${user.name} "
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error al cargar los datos del usuario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchDestinosFiltrados(nombreCategoria: String) {
        val call = RetrofitClient.vueloApiService.getVuelosByCategoria(nombreCategoria)

        call.enqueue(object : Callback<List<Vuelo>> {
            override fun onResponse(
                call: Call<List<Vuelo>>,
                response: Response<List<Vuelo>>
            ) {
                if (response.isSuccessful) {
                    destinosActuales = response.body() ?: emptyList()
                    destinoAdapter.updateFavoritos(destinosActuales, favoritosIds)
                } else {
                    Toast.makeText(this@UsuarioLogeadoInicial, "Error cargando datos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Vuelo>>, t: Throwable) {
                Toast.makeText(this@UsuarioLogeadoInicial, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun cargarFavoritos() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("favorites")

        ref.get().addOnSuccessListener { snap ->
            favoritosIds.clear()
            snap.children.forEach { favoritosIds.add(it.key!!) }
            destinoAdapter.updateFavoritos(destinosActuales, favoritosIds)
        }
    }

    private fun buscarDestinosPorNombre(nombre: String) {
        val call = RetrofitClient.vueloApiService.searchVuelosByNombre(nombre)

        call.enqueue(object : Callback<List<Vuelo>> {
            override fun onResponse(
                call: Call<List<Vuelo>>,
                response: Response<List<Vuelo>>
            ) {
                if (response.isSuccessful) {
                    val destinosFiltrados = response.body() ?: emptyList()
                    destinoAdapter.updateFavoritos(destinosFiltrados, favoritosIds)
                } else {
                    Toast.makeText(this@UsuarioLogeadoInicial, "Error en búsqueda", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Vuelo>>, t: Throwable) {
                Toast.makeText(this@UsuarioLogeadoInicial, "Error al buscar", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun toggleFavorite(vuelo: Vuelo) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("favorites")
            .child(vuelo.id)

        if (favoritosIds.contains(vuelo.id)) {
            ref.removeValue()
        } else {
            ref.setValue(true)
        }

        // Actualiza UI localmente
        cargarFavoritos()
    }
    override fun onResume() {
        super.onResume()
        cargarFavoritos()
    }


}