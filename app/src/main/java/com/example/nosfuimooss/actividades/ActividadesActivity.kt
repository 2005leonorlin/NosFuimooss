package com.example.nosfuimooss.actividades

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.NosFuimooss.R
import com.example.nosfuimooss.Adapter.actividades.ActividadesAdapter
import com.example.nosfuimooss.api.RetrofitClient
import com.example.nosfuimooss.boleto.VuelosActivity
import com.example.nosfuimooss.model.Actividad
import com.example.nosfuimooss.navegador.Calendario
import com.example.nosfuimooss.navegador.Favoritos
import com.example.nosfuimooss.navegador.UsuarioPerfil
import com.example.nosfuimooss.usuariologeado.UsuarioLogeadoInicial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ActividadesActivity : AppCompatActivity() {
    private lateinit var activitiesRecyclerView: RecyclerView
    private lateinit var activitiesAdapter: ActividadesAdapter
    private lateinit var usernameText: TextView
    private var destinoUbicacion: String? = null

    private val auth = FirebaseAuth.getInstance()
    private val activitiesList = ArrayList<Actividad>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_actividades)

        // Obtener el destino desde el intent
        destinoUbicacion = intent.getStringExtra("ubicacionDestino")

        initializeViews()
        setupRecyclerView()
        loadUserInfo()
        loadActivities()
        setupClickListeners()
    }

    private fun initializeViews() {
        activitiesRecyclerView = findViewById(R.id.activities_recycler_view)
        usernameText = findViewById(R.id.username_text)
    }

    private fun setupRecyclerView() {
        activitiesAdapter = ActividadesAdapter(activitiesList) { actividad ->
            // Click en una actividad - navegar a detalles
            val intent = Intent(this, DetalleActividadActivity::class.java)
            intent.putExtra("actividad", actividad)
            startActivity(intent)
        }

        activitiesRecyclerView.layoutManager = LinearLayoutManager(this)
        activitiesRecyclerView.adapter = activitiesAdapter
    }

    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val database = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(userId)

            database.get().addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(com.example.nosfuimooss.model.User::class.java)
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

    private fun loadActivities() {
        val call = RetrofitClient.actividadApiService.getAllActividades()

        call.enqueue(object : Callback<List<Actividad>> {
            override fun onResponse(call: Call<List<Actividad>>, response: Response<List<Actividad>>) {
                if (response.isSuccessful) {
                    val allActivities = response.body() ?: emptyList()

                    // Filtrar actividades por ubicación si se proporcionó un destino
                    val filteredActivities = if (destinoUbicacion != null) {
                        allActivities.filter { actividad ->
                            actividad.ubicacion?.contains(destinoUbicacion!!, ignoreCase = true) == true
                        }
                    } else {
                        allActivities
                    }

                    activitiesList.clear()
                    activitiesList.addAll(filteredActivities)
                    activitiesAdapter.notifyDataSetChanged()

                    if (filteredActivities.isEmpty() && destinoUbicacion != null) {
                        Toast.makeText(
                            this@ActividadesActivity,
                            "No se encontraron actividades para $destinoUbicacion",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@ActividadesActivity,
                        "Error al cargar actividades",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<Actividad>>, t: Throwable) {
                Toast.makeText(
                    this@ActividadesActivity,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupClickListeners() {
        // Navigation listeners
        findViewById<ImageView>(R.id.nav_home).setOnClickListener {
            navigateToMain()
        }

        findViewById<ImageView>(R.id.nav_flight).setOnClickListener {
            navigateToFlights()
        }

        findViewById<ImageView>(R.id.nav_activities).setOnClickListener {
            // Ya estamos en actividades, no hacer nada o refrescar
        }

        findViewById<ImageView>(R.id.nav_heart).setOnClickListener {
            navigateToFavorites()
        }

        findViewById<ImageView>(R.id.nav_profile).setOnClickListener {
            navigateToProfile()
        }

        findViewById<ImageView>(R.id.ic_calendar).setOnClickListener {
            navigateToCalendar()
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
}