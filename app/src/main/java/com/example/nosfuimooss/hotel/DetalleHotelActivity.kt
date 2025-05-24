package com.example.nosfuimooss.hotel

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Hotel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DetalleHotelActivity : AppCompatActivity() {

    private lateinit var tvHotelName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvDireccion: TextView
    private lateinit var tvFechaEntrada: TextView
    private lateinit var tvFechaSalida: TextView
    private lateinit var tvPersonas: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var btnReservar: Button
    private lateinit var vpHotelImages: ViewPager2
    private lateinit var tabLayout: TabLayout

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            val itemCount = vpHotelImages.adapter?.itemCount ?: 0
            if (itemCount > 1) {
                val nextItem = (vpHotelImages.currentItem + 1) % itemCount
                vpHotelImages.setCurrentItem(nextItem, true)

                // Programar el próximo desplazamiento
                autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY)
            }
        }
    }

    companion object {
        private const val AUTO_SCROLL_DELAY = 3000L // 3 segundos entre cambios de imagen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_detalle_hotel)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inicializar vistas
        tvHotelName = findViewById(R.id.tvHotelName)
        tvLocation = findViewById(R.id.tvLocation)
        tvPrice = findViewById(R.id.tvPrice)
        tvDireccion = findViewById(R.id.tvDireccion)
        tvFechaEntrada = findViewById(R.id.tvFechaEntrada)
        tvFechaSalida = findViewById(R.id.tvFechaSalida)
        tvPersonas = findViewById(R.id.tvPersonas)
        ratingBar = findViewById(R.id.ratingBar)
        vpHotelImages = findViewById(R.id.vpHotelImages)
        btnReservar = findViewById(R.id.btnReservar)


        // Si has agregado un TabLayout para los indicadores de posición
        tabLayout = findViewById(R.id.tabLayoutIndicator)

        // Obtener datos
        val hotel = intent.getSerializableExtra("hotel") as Hotel
        val adultos = intent.getIntExtra("adultos", 1)
        val ninos = intent.getIntExtra("ninos", 0)
        val destino = intent.getStringExtra("destino") ?: ""
        val fechaEntrada = intent.getStringExtra("fechaEntrada") ?: ""
        val fechaSalida = intent.getStringExtra("fechaSalida") ?: ""

        // CORRECCIÓN: Recuperar correctamente el precio ajustado
        // En lugar de usar getDoubleExtra con un valor por defecto que sobrescribe el valor,
        // verificamos primero si el extra existe y solo entonces lo usamos
        val precioFinal = if (intent.hasExtra("precioFinal")) {
            intent.getDoubleExtra("precioFinal", 0.0)
        } else {
            // Calcular el precio si no se ha pasado
            val totalPersonas = adultos + ninos
            val adicionales = (totalPersonas - 2).coerceAtLeast(0)
            hotel.precioNoche + (adicionales * 22)
        }

        // Usar la lista de imágenes si está disponible, de lo contrario usar la imagen principal
        val imagenes = hotel.imagenes?.takeIf { it.isNotEmpty() } ?: listOf(hotel.imagenUrl)

        // Actualizar la interfaz
        tvHotelName.text = hotel.nombre
        tvLocation.text = hotel.ubicacion
        tvPrice.text = "Precio: ${precioFinal.toInt()}€ / noche"
        tvDireccion.text = hotel.direccion ?: "Dirección no disponible"
        tvFechaEntrada.text = "Entrada: $fechaEntrada"
        tvFechaSalida.text = "Salida: $fechaSalida"
        tvPersonas.text = "$adultos adultos, $ninos niños"
        ratingBar.rating = hotel.estrellas.toFloat()

        // Configurar ViewPager2 con adapter
        vpHotelImages.adapter = HotelImagePagerAdapter(imagenes)

        // Aplicar transformación para dar efecto de carrusel
        vpHotelImages.offscreenPageLimit = 3

        // Si hay más de una imagen, entonces mostramos los indicadores y activamos el autodesplazamiento
        if (imagenes.size > 1) {
            // Configurar los indicadores solo si hay más de 1 imagen
            TabLayoutMediator(tabLayout, vpHotelImages) { _, _ ->
                // No necesitamos configurar nada para los tabs, solo usarlos como indicadores
            }.attach()

            // Iniciar autoscroll después de un retraso inicial
            startAutoScroll()
        } else {
            // Si solo hay una imagen, ocultamos los indicadores
            tabLayout.visibility = android.view.View.GONE
        }

        // Botón reservar - MODIFICADO para abrir la actividad ElijeTuEstancia
        btnReservar.setOnClickListener {
            if (!hotel.disponible) {
                Toast.makeText(this, "Este hotel no está disponible", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intentEstancia = Intent(this, ElijeTuEstanciaActivity::class.java).apply {
                putExtra("hotel", hotel)
                putExtra("adultos", adultos)
                putExtra("ninos", ninos)
                putExtra("destino", destino)
                putExtra("fechaEntrada", fechaEntrada)
                putExtra("fechaSalida", fechaSalida)
                putExtra("precioFinal", precioFinal)
            }
            startActivity(intentEstancia)
        }
    }

    private fun startAutoScroll() {
        // Eliminar cualquier callback pendiente para evitar duplicados
        stopAutoScroll()

        // Programar el primer desplazamiento después del retraso inicial
        autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY)
    }

    private fun stopAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }

    override fun onResume() {
        super.onResume()
        if (vpHotelImages.adapter?.itemCount ?: 0 > 1) {
            startAutoScroll()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}