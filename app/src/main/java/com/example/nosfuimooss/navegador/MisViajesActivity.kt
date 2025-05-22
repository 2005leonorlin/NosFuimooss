package com.example.nosfuimooss.navegador


import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.NosFuimooss.R
import com.example.nosfuimooss.misvuelos.BoletosFragment
import com.example.nosfuimooss.misvuelos.HotelesFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase



class MisViajesActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_mis_viajes)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Verificar si hay usuario logueado
        if (auth.currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para ver tus viajes", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Inicializar vistas
        initViews()
        setupViewPager()
        setupBottomNavigation()
        loadUserInfo()

        // Manejar la selección de tab si viene desde confirmación
        handleTabSelection()
    }

    private fun initViews() {
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        // Configurar listener para los botones de navegación superior
        findViewById<ImageView>(R.id.ic_search).setOnClickListener {
            Toast.makeText(this, "Búsqueda no implementada", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.ic_calendar).setOnClickListener {
            Toast.makeText(this, "Calendario no implementado", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.ic_logo).setOnClickListener {
            finish()
        }
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        // Conectar TabLayout con ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Boletos"
                1 -> "Hoteles"
                else -> ""
            }
        }.attach()
    }

    private fun handleTabSelection() {
        // Verificar si se debe seleccionar una pestaña específica
        val tabPosition = intent.getIntExtra("tab_position", -1)
        if (tabPosition != -1) {
            viewPager.currentItem = tabPosition
            Log.d("MisViajesActivity", "Seleccionando tab: $tabPosition")
        }

        // Verificar si hay IDs de nuevas reservas para mostrar
        val nuevaReservaId = intent.getStringExtra("nuevaReservaId")
        val nuevaReservaHotelId = intent.getStringExtra("nuevaReservaHotelId")

        if (nuevaReservaId != null) {
            Log.d("MisViajesActivity", "Nueva reserva de vuelo: $nuevaReservaId")
            // Seleccionar tab de boletos
            viewPager.currentItem = 0
        }

        if (nuevaReservaHotelId != null) {
            Log.d("MisViajesActivity", "Nueva reserva de hotel: $nuevaReservaHotelId")
            // Seleccionar tab de hoteles
            viewPager.currentItem = 1
        }
    }

    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userNameText = findViewById<TextView>(R.id.user_name_text)
            val database = FirebaseDatabase.getInstance().reference
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

    private fun setupBottomNavigation() {
        // Home
        findViewById<android.widget.ImageView>(R.id.nav_home).setOnClickListener {
            finish()
        }

        // Vuelos
        findViewById<android.widget.ImageView>(R.id.nav_flight).setOnClickListener {
            Toast.makeText(this, "Búsqueda de vuelos", Toast.LENGTH_SHORT).show()
        }

        // Maleta (Mis viajes) - Ya estamos aquí
        findViewById<android.widget.ImageView>(R.id.nav_moon).setOnClickListener {
            // Estamos en esta pantalla, no hacemos nada
        }

        // Favoritos
        findViewById<android.widget.ImageView>(R.id.nav_heart).setOnClickListener {
            Toast.makeText(this, "Favoritos no implementado", Toast.LENGTH_SHORT).show()
        }

        // Perfil
        findViewById<android.widget.ImageView>(R.id.nav_profile).setOnClickListener {
            Toast.makeText(this, "Perfil no implementado", Toast.LENGTH_SHORT).show()
        }
    }

    // Adapter para ViewPager2
    private class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> BoletosFragment()
                1 -> HotelesFragment()
                else -> BoletosFragment()
            }
        }
    }
}