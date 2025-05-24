package com.example.nosfuimooss.navegador

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.NosFuimooss.R
import com.example.nosfuimooss.hotelyboleto.ElegirFragmentHotelesActivity
import com.example.nosfuimooss.hotelyboleto.ElegirFragmentVuelosActivity
import com.example.nosfuimooss.meFaltaHacer.UsuarioPerfil
import com.example.nosfuimooss.usuariologeado.UsuarioLogeadoInicial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class ElegirVuelo : AppCompatActivity() {

    private lateinit var buttonVuelos: Button
    private lateinit var buttonHoteles: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        enableEdgeToEdge()
        setContentView(R.layout.activity_elegir_vuelo)

        // Configurar window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar componentes
        initializeButtons()
        loadUserData()

        // Configurar listeners
        setupButtonListeners()
        setupNavigation()
    }

    private fun initializeButtons() {
        buttonVuelos = findViewById(R.id.button_vuelos)
        buttonHoteles = findViewById(R.id.button_hoteles)
    }
    private fun loadUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userNameText = findViewById<TextView>(R.id.user_name_text)

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(com.example.nosfuimooss.model.User::class.java)
                userNameText.text = if (user?.name?.isNotBlank() == true)
                    "${user.name}"
                else
                    "Hola, usuario"
            }
            .addOnFailureListener {
                userNameText.text = "Hola, usuario"
            }
    }



    private fun setupButtonListeners() {
        buttonVuelos.setOnClickListener {
            // Navegar a la actividad de vuelos
            val intent = Intent(this, ElegirFragmentVuelosActivity::class.java)
            startActivity(intent)
        }

        buttonHoteles.setOnClickListener {
            // Navegar a la actividad de hoteles
            val intent = Intent(this, ElegirFragmentHotelesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupNavigation() {
        findViewById<ImageView>(R.id.nav_home).setOnClickListener {
            Toast.makeText(this, "Destinos", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, UsuarioLogeadoInicial::class.java))
        }
        findViewById<ImageView>(R.id.nav_flight).setOnClickListener {
            Toast.makeText(this, "Búsqueda de vuelos", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageView>(R.id.nav_moon).setOnClickListener {
            Toast.makeText(this, "Mis Viajes", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MisViajesActivity::class.java))
        }
        findViewById<ImageView>(R.id.nav_heart).setOnClickListener {
            Toast.makeText(this, "Favoritos", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Favoritos::class.java))
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