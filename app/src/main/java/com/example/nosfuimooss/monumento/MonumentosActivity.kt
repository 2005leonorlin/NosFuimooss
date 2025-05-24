package com.example.nosfuimooss.monumento

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.NosFuimooss.R
import com.example.nosfuimooss.Adapter.MonumentosAdapter
import com.example.nosfuimooss.api.RetrofitClient
import com.example.nosfuimooss.boleto.VuelosActivity
import com.example.nosfuimooss.navegador.Calendario
import com.example.nosfuimooss.meFaltaHacer.UsuarioPerfil
import com.example.nosfuimooss.model.Monumento
import com.example.nosfuimooss.model.User
import com.example.nosfuimooss.navegador.Favoritos
import com.example.nosfuimooss.navegador.MisViajesActivity
import com.example.nosfuimooss.usuariologeado.UsuarioLogeadoInicial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MonumentosActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var monumentosAdapter: MonumentosAdapter
    private lateinit var usernameText: TextView
    private val monumentosList = mutableListOf<Monumento>()
    private val auth = FirebaseAuth.getInstance()

    private var ubicacionDestino: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_monumentos)

        // Obtener la ubicación del destino desde el Intent
        ubicacionDestino = intent.getStringExtra("ubicacionDestino")

        initializeViews()
        setupRecyclerView()
        loadUserInfo()
        loadMonumentos()
        setupClickListeners()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.monuments_recycler_view)
        usernameText = findViewById(R.id.username_text)
    }

    private fun setupRecyclerView() {
        monumentosAdapter = MonumentosAdapter(monumentosList) { monumento ->
            // Navegar al detalle del monumento
            onMonumentoClick(monumento)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = monumentosAdapter
    }

    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val database = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(userId)

            database.get().addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    usernameText.text = user.name
                } else {
                    usernameText.text = "Usuario"
                }
            }.addOnFailureListener {
                usernameText.text = "Usuario"
            }
        }
    }

    private fun loadMonumentos() {
        if (!ubicacionDestino.isNullOrEmpty()) {
            // Log para debugging
            android.util.Log.d("MonumentosActivity", "Cargando monumentos para ubicación: '$ubicacionDestino'")
            loadMonumentosByLocation(ubicacionDestino!!)
        } else {
            android.util.Log.d("MonumentosActivity", "No hay ubicación específica, cargando todos los monumentos")
            loadAllMonumentos()
        }
    }

    private fun loadMonumentosByLocation(ubicacion: String) {
        // Limpiar la ubicación de espacios y normalizar
        val ubicacionLimpia = ubicacion.trim()

        android.util.Log.d("MonumentosActivity", "Llamando API con ubicación: '$ubicacionLimpia'")

        val call = RetrofitClient.monumentoApiService.getMonumentosByUbicacion(ubicacionLimpia)

        call.enqueue(object : Callback<List<Monumento>> {
            override fun onResponse(call: Call<List<Monumento>>, response: Response<List<Monumento>>) {
                android.util.Log.d("MonumentosActivity", "Respuesta API - Código: ${response.code()}")

                if (response.isSuccessful) {
                    val monumentos = response.body()
                    android.util.Log.d("MonumentosActivity", "Monumentos recibidos: ${monumentos?.size ?: 0}")

                    if (monumentos != null && monumentos.isNotEmpty()) {
                        monumentosList.clear()
                        monumentosList.addAll(monumentos)
                        monumentosAdapter.notifyDataSetChanged()

                        Toast.makeText(
                            this@MonumentosActivity,
                            "Se encontraron ${monumentos.size} monumentos en $ubicacionLimpia",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Si no hay monumentos para la ubicación específica, cargar todos
                        android.util.Log.d("MonumentosActivity", "No hay monumentos para la ubicación, cargando todos")
                        loadAllMonumentos()

                        Toast.makeText(
                            this@MonumentosActivity,
                            "No se encontraron monumentos para $ubicacionLimpia. Mostrando todos los monumentos.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    android.util.Log.e("MonumentosActivity", "Error en respuesta: ${response.errorBody()?.string()}")
                    Toast.makeText(
                        this@MonumentosActivity,
                        "Error al cargar monumentos para $ubicacionLimpia",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Cargar todos los monumentos como fallback
                    loadAllMonumentos()
                }
            }

            override fun onFailure(call: Call<List<Monumento>>, t: Throwable) {
                android.util.Log.e("MonumentosActivity", "Error de conexión: ${t.message}")
                Toast.makeText(
                    this@MonumentosActivity,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()

                // Cargar todos los monumentos como fallback
                loadAllMonumentos()
            }
        })
    }

    private fun loadAllMonumentos() {
        val call = RetrofitClient.monumentoApiService.getAllMonumentos()

        call.enqueue(object : Callback<List<Monumento>> {
            override fun onResponse(call: Call<List<Monumento>>, response: Response<List<Monumento>>) {
                if (response.isSuccessful) {
                    val monumentos = response.body()
                    if (monumentos != null) {
                        monumentosList.clear()
                        monumentosList.addAll(monumentos)
                        monumentosAdapter.notifyDataSetChanged()
                    }
                } else {
                    Toast.makeText(
                        this@MonumentosActivity,
                        "Error al cargar monumentos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<Monumento>>, t: Throwable) {
                Toast.makeText(
                    this@MonumentosActivity,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun onMonumentoClick(monumento: Monumento) {
        // Navegar al DetalleMonumentoActivity
        val intent = Intent(this, DetalleMonumentoActivity::class.java)
        intent.putExtra("monumento", monumento)
        startActivity(intent)
    }

    private fun setupClickListeners() {
        // Navegación inferior
        findViewById<ImageView>(R.id.nav_home).setOnClickListener {
            navigateToMain()
        }

        findViewById<ImageView>(R.id.nav_flight).setOnClickListener {
            navigateToFlights()
        }

        findViewById<ImageView>(R.id.nav_moon).setOnClickListener {
            navigateToNights()
        }

        findViewById<ImageView>(R.id.nav_heart).setOnClickListener {
            navigateToFavorites()
        }

        findViewById<ImageView>(R.id.nav_profile).setOnClickListener {
            navigateToProfile()
        }

        // Calendario
        findViewById<ImageView>(R.id.ic_calendar).setOnClickListener {
            navigateToCalendar()
        }

        // NUEVO: Click listener para el botón del mapa
        findViewById<LinearLayout>(R.id.map_section).setOnClickListener {
            navigateToMap()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, UsuarioLogeadoInicial::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToFlights() {
        val intent = Intent(this, VuelosActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToNights() {
        val intent = Intent(this, MisViajesActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToFavorites() {
        val intent = Intent(this, Favoritos::class.java)
        startActivity(intent)
    }

    private fun navigateToProfile() {
        val intent = Intent(this, UsuarioPerfil::class.java)
        startActivity(intent)
    }

    private fun navigateToCalendar() {
        val intent = Intent(this, Calendario::class.java)
        startActivity(intent)
    }

    private fun navigateToMap() {
        val intent = Intent(this, MapaMonumentosActivity::class.java)
        // Pasar la ubicación del destino al mapa si existe
        if (!ubicacionDestino.isNullOrEmpty()) {
            intent.putExtra("ubicacionDestino", ubicacionDestino)
        }
        startActivity(intent)
    }
}