package com.example.NosFuimooss

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.NosFuimooss.databinding.ActivityWelcomBinding
import kotlin.random.Random

class WelcomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomBinding

    private val images = listOf(
        R.drawable.imagen_1,
        R.drawable.imagen_2,
        R.drawable.imagen_3,
        R.drawable.imagen_4,
        R.drawable.imagen_5,
        R.drawable.imagen_6,
        R.drawable.imagen_7,
        R.drawable.imagen_8,
        R.drawable.imagen_9,
        R.drawable.imagen_10,
        R.drawable.imagen_11,
        R.drawable.imagen_12,
        R.drawable.imagen_13,
        R.drawable.imagen_14,
        R.drawable.imagen_15
    )

    private var currentIndex = 0
    private val selectedImages = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar View Binding
        binding = ActivityWelcomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Selecciona aleatoriamente 3 imágenes
        selectRandomImages()

        // Mostrar la primera imagen al inicio
        binding.imageViewWelcome.setImageResource(selectedImages[currentIndex])

        // Configurar el botón para cambiar las imágenes
        binding.buttonNext.setOnClickListener {
            currentIndex++
            if (currentIndex < selectedImages.size) {
                binding.imageViewWelcome.setImageResource(selectedImages[currentIndex])
            } else {
                // Cuando se han mostrado todas las imágenes, inicia la actividad principal
                startActivity(Intent(this, UsuarioNoLogeadoInicioActivity::class.java))
                finish()
            }
        }

        // Mantener la configuración de insets para Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Función para seleccionar aleatoriamente 3 imágenes
    private fun selectRandomImages() {
        val randomIndexes = mutableSetOf<Int>()
        while (randomIndexes.size < 3) {
            val randomIndex = Random.nextInt(images.size)
            randomIndexes.add(randomIndex)
        }

        // Asignar las imágenes seleccionadas a la lista
        selectedImages.addAll(randomIndexes.map { images[it] })
    }
}