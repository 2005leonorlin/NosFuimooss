package com.example.nosfuimooss

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
import com.example.nosfuimooss.meFaltaHacer.Calendario
import com.example.nosfuimooss.meFaltaHacer.ElegirVuelo
import com.example.nosfuimooss.navegador.Favoritos
import com.example.nosfuimooss.meFaltaHacer.ReservarHotel
import com.example.nosfuimooss.meFaltaHacer.UsuarioPerfil
import com.example.nosfuimooss.model.Categoria
import com.example.nosfuimooss.model.Destino
import com.example.nosfuimooss.model.User
import com.example.nosfuimooss.sesion.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class UsuarioLogeadoInicial : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var categoriaAdapter: CategoriaAdapter
    private lateinit var destinoAdapter: DestinoAdapter
    private var favoritosIds: MutableList<String> = mutableListOf()
    private var destinosActuales: List<Destino> = emptyList()
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
        firestore = FirebaseFirestore.getInstance()

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
            startActivity(Intent(this, ReservarHotel::class.java))
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
        firestore.collection("Viajes")
            .whereArrayContains("categoria", nombreCategoria)
            .get()
            .addOnSuccessListener { result ->
                destinosActuales = result.map { document ->
                    val id = document.id
                    val principal = document.getString("principal") ?: ""
                    val imagenes = document.get("imagenes") as? List<String> ?: emptyList()
                    val imagenPrincipal = if (principal.isNotEmpty()) principal else imagenes.firstOrNull() ?: ""

                    Destino(
                        id = id,
                        nombre = document.getString("nombre") ?: "",
                        descripcion = document.getString("descripcion") ?: "",
                        categoria = document.get("categoria") as? List<String> ?: emptyList(),
                        imagenes = listOf(imagenPrincipal),
                        bandera = document.getString("bandera") ?: ""
                    )
                }

                destinoAdapter.updateFavoritos(destinosActuales, favoritosIds)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al cargar destinos: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buscarDestinosPorNombre(nombre: String) {
        firestore.collection("Viajes")
            .get()
            .addOnSuccessListener { result ->
                val destinosFiltrados = result.mapNotNull { doc ->
                    doc.getString("nombre")
                        ?.takeIf { it.lowercase().contains(nombre.lowercase()) }
                        ?.let { nombreOk ->
                            Destino(
                                id          = doc.id,
                                nombre      = nombreOk,
                                descripcion = doc.getString("descripcion") ?: "",
                                categoria   = doc.get("categoria") as? List<String> ?: emptyList(),
                                imagenes    = listOf(
                                    doc.getString("principal")
                                        .takeIf { !it.isNullOrEmpty() }
                                        ?: (doc.get("imagenes") as? List<String>).orEmpty().firstOrNull()
                                            .orEmpty()
                                ),
                                bandera     = doc.getString("bandera") ?: ""
                            )
                        }
                }

                destinoAdapter.updateFavoritos(destinosFiltrados, favoritosIds)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error buscando destinos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleFavorite(destino: Destino) {
        val userId = auth.currentUser?.uid ?: return
        val favRef = firestore.collection("users").document(userId).collection("favorites").document(destino.id)

        if (favoritosIds.contains(destino.id)) {
            // Eliminar de favoritos
            favRef.delete().addOnSuccessListener {
                favoritosIds.remove(destino.id)
                destinoAdapter.updateFavoritos(destinosActuales, favoritosIds)
            }
        } else {
            // Añadir a favoritos
            val data = mapOf(
                "destinationId" to destino.id,
                "addedAt" to System.currentTimeMillis()
            )
            favRef.set(data).addOnSuccessListener {
                favoritosIds.add(destino.id)
                destinoAdapter.updateFavoritos(destinosActuales, favoritosIds)
            }
        }
    }

}