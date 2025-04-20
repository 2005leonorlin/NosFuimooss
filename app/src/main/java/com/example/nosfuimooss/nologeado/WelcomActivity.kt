package com.example.nosfuimooss.nologeado

import android.content.Intent
import android.os.Bundle

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.NosFuimooss.R
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
        supportActionBar?.hide()
        enableEdgeToEdge()

        binding = ActivityWelcomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        selectRandomImages()
        binding.imageViewWelcome.setImageResource(selectedImages[currentIndex])

        binding.buttonNext.setOnClickListener {
            currentIndex++
            if (currentIndex < selectedImages.size) {
                binding.imageViewWelcome.setImageResource(selectedImages[currentIndex])
            } else {

                startActivity(Intent(this, UsuarioNoLogeadoInicioActivity::class.java))
                finish()
            }
        }


        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    private fun selectRandomImages() {
        val randomIndexes = mutableSetOf<Int>()
        while (randomIndexes.size < 3) {
            val randomIndex = Random.nextInt(images.size)
            randomIndexes.add(randomIndex)
        }


        selectedImages.addAll(randomIndexes.map { images[it] })
    }
}