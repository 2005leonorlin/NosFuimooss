package com.example.nosfuimooss.usuariologeado

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.hotel.ReservarHotel
import com.example.nosfuimooss.api.RetrofitClient
import com.example.nosfuimooss.meFaltaHacer.ActividadesActivity
import com.example.nosfuimooss.navegador.BuscadorActivity
import com.example.nosfuimooss.meFaltaHacer.Calendario
import com.example.nosfuimooss.navegador.Favoritos
import com.example.nosfuimooss.navegador.MisViajesActivity
import com.example.nosfuimooss.meFaltaHacer.MonumentosActivity
import com.example.nosfuimooss.meFaltaHacer.PlanesActivity
import com.example.nosfuimooss.meFaltaHacer.UsuarioPerfil
import com.example.nosfuimooss.boleto.VuelosActivity
import com.example.nosfuimooss.model.Vuelo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetalleVuelos : AppCompatActivity() {
    private lateinit var userNameText: TextView
    private lateinit var destinationNameText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var flagIcon: ImageView
    private lateinit var favoriteButton: ImageView
    private lateinit var imageCarousel: ViewPager2
    private lateinit var indicatorLayout: LinearLayout


    private val auth = FirebaseAuth.getInstance()

    private var destinationId: String? = null
    private var isFavorite = false
    private var imagesList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.detalle_vuelos)


        // Initialize views
        userNameText = findViewById(R.id.user_name_text)
        destinationNameText = findViewById(R.id.destination_name)
        descriptionText = findViewById(R.id.description_text)
        flagIcon = findViewById(R.id.flag_icon)
        favoriteButton = findViewById(R.id.favorite_button)
        imageCarousel = findViewById(R.id.image_carousel)
        indicatorLayout = findViewById(R.id.indicator_layout)

        destinationId = intent.getStringExtra("destinoId")

        if (destinationId != null) {
            loadDestinationData(destinationId!!)
        } else {
            Toast.makeText(this, "No se recibió el ID del destino", Toast.LENGTH_SHORT).show()
            finish()
        }

        loadUserInfo()
        destinationId?.let { loadDestinationData(it) }
        setupClickListeners()
        setupInitialCarousel()
    }

    private fun setupInitialCarousel() {

        indicatorLayout.removeAllViews()

        for (i in 0 until 5) {
            val indicator = View(this)
            val size = resources.getDimensionPixelSize(R.dimen.indicator_size)
            val params = LinearLayout.LayoutParams(size, size)
            val margin = resources.getDimensionPixelSize(R.dimen.indicator_margin)
            params.setMargins(margin, margin, margin, margin)
            indicator.layoutParams = params


            indicator.background = if (i == 0) {
                ContextCompat.getDrawable(this, R.drawable.active_dot_background)
            } else {
                ContextCompat.getDrawable(this, R.drawable.inactive_dot_background)
            }

            indicatorLayout.addView(indicator)
        }
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

    private fun loadDestinationData(destId: String) {
        val call = RetrofitClient.vueloApiService.getVueloById(destId)

        call.enqueue(object : Callback<Vuelo> {
            override fun onResponse(call: Call<Vuelo>, response: Response<Vuelo>) {
                if (response.isSuccessful) {
                    val destino = response.body()
                    if (destino != null) {
                        destinationNameText.text = destino.nombre.uppercase()
                        descriptionText.text = destino.descripcion

                        Glide.with(this@DetalleVuelos)
                            .load(destino.bandera)
                            .into(flagIcon)

                        imagesList.clear()
                        imagesList.addAll(destino.imagenes)
                        setupImageCarousel()

                        checkIfFavorite(destino.id)
                    }
                } else {
                    Toast.makeText(this@DetalleVuelos, "No se pudo cargar el destino", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Vuelo>, t: Throwable) {
                Toast.makeText(this@DetalleVuelos, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun setupImageCarousel() {
        val adapter = ImageCarouselAdapter(imagesList)
        imageCarousel.adapter = adapter

        // Limpiar indicadores previos
        indicatorLayout.removeAllViews()

        // Configurar indicadores basados en la cantidad real de imágenes
        for (i in imagesList.indices) {
            val indicator = View(this)
            val size = 24 // Tamaño en px
            val params = LinearLayout.LayoutParams(size, size)
            val margin = 12 // Margen en px
            params.setMargins(margin, margin, margin, margin)
            indicator.layoutParams = params

            // Asignar el fondo según si es el primer punto (activo) o no
            indicator.background = if (i == 0) {
                ContextCompat.getDrawable(this, R.drawable.indicator_active)
            } else {
                ContextCompat.getDrawable(this, R.drawable.indicator_inactive)
            }

            indicatorLayout.addView(indicator)
        }

        imageCarousel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
            }
        })
    }

    private fun updateIndicators(position: Int) {
        val childCount = indicatorLayout.childCount
        for (i in 0 until childCount) {
            val indicator = indicatorLayout.getChildAt(i) as View
            indicator.background = if (i == position) {
                ContextCompat.getDrawable(this, R.drawable.indicator_active)
            } else {
                ContextCompat.getDrawable(this, R.drawable.indicator_inactive)
            }
        }
    }

    private fun checkIfFavorite(destId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("favorites")
            .child(destId)

        ref.get().addOnSuccessListener { snapshot ->
            isFavorite = snapshot.exists()
            updateFavoriteButton()
        }
    }

    private fun updateFavoriteButton() {

        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_corazon_completo)

        } else {
            favoriteButton.setImageResource(R.drawable.ic_megusta)

        }
    }

    private fun toggleFavorite() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val destId = destinationId ?: return
        val ref = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("favorites")
            .child(destId)

        if (isFavorite) {
            ref.removeValue().addOnSuccessListener {
                isFavorite = false
                updateFavoriteButton()
            }
        } else {
            ref.setValue(true).addOnSuccessListener {
                isFavorite = true
                updateFavoriteButton()
            }
        }
    }

    private fun setupClickListeners() {

        favoriteButton.setOnClickListener {
            toggleFavorite()
        }


        val descriptionSection = findViewById<View>(R.id.description_section)
        descriptionSection.setOnClickListener {
            if (descriptionText.visibility == View.VISIBLE) {
                descriptionText.visibility = View.GONE
            } else {
                descriptionText.visibility = View.VISIBLE
            }
        }

        findViewById<View>(R.id.hotel_button).setOnClickListener {

            val intent = Intent(this, ReservarHotel::class.java)
            val ubicacion = destinationNameText.text.toString().lowercase().capitalize()
            intent.putExtra("ubicacionDestino", ubicacion)
            startActivity(intent)
        }

        findViewById<View>(R.id.activities_button).setOnClickListener {
            val intent = Intent(this, ActividadesActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.monuments_button).setOnClickListener {
            val intent = Intent(this, MonumentosActivity::class.java)
            startActivity(intent)
        }


        findViewById<View>(R.id.plans_button).setOnClickListener {
            val intent = Intent(this, PlanesActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.flights_button).setOnClickListener {
            val intent = Intent(this, VuelosActivity::class.java)
            val destinoSeleccionado = destinationNameText.text.toString().lowercase().capitalize()
            intent.putExtra("destino", destinoSeleccionado)
            startActivity(intent)
        }

        findViewById<View>(R.id.nav_home_container).setOnClickListener {
            navigateToMain()
        }

        findViewById<View>(R.id.nav_flight_container).setOnClickListener {
            navigateToFlights()
        }

        findViewById<View>(R.id.nav_moon_container).setOnClickListener {
            navigateToNights()
        }

        findViewById<View>(R.id.nav_heart_container).setOnClickListener {
            navigateToFavorites()
        }

        findViewById<View>(R.id.nav_profile_container).setOnClickListener {
            navigateToProfile()
        }


        findViewById<View>(R.id.ic_search).setOnClickListener {
            navigateToSearch()
        }

        findViewById<View>(R.id.ic_calendar).setOnClickListener {
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

    private fun navigateToSearch() {
        val intent = Intent(this, BuscadorActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCalendar() {
        val intent = Intent(this, Calendario::class.java)
        startActivity(intent)
    }
}

// Adapter for image carousel
class ImageCarouselAdapter(private val imagesList: List<String>) :
    RecyclerView.Adapter<ImageCarouselAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.carousel_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imagesList[position]
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return imagesList.size
    }
}