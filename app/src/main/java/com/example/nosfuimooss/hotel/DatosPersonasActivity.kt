package com.example.nosfuimooss.hotel

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Hotel
import com.google.android.material.textfield.TextInputEditText

class DatosPersonasActivity : AppCompatActivity() {
    private lateinit var containerPersonas: LinearLayout
    private lateinit var btnConfirmarReserva: Button

    private var hotel: Hotel? = null
    private var tipoHabitacion: String = ""
    private var precioPorNoche: Double = 0.0
    private var precioTotal: Double = 0.0
    private var adultos: Int = 1
    private var ninos: Int = 0
    private var fechaEntrada: String = ""
    private var fechaSalida: String = ""
    private var numNoches: Int = 1
    private var destino: String = ""

    // Listas para manejar los campos para cada persona
    private val personasNombre = ArrayList<TextInputEditText>()
    private val personasApellidos = ArrayList<TextInputEditText>()
    private val personasDni = ArrayList<TextInputEditText>()
    private val personasEdad = ArrayList<TextInputEditText>()
    private val personasEmail = ArrayList<TextInputEditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_datos_personas)

        // Inicializar vistas
        containerPersonas = findViewById(R.id.containerPersonas)
        btnConfirmarReserva = findViewById(R.id.btnConfirmarReserva)

        // Obtener datos del Intent
        hotel = intent.getSerializableExtra("hotel") as? Hotel
        tipoHabitacion = intent.getStringExtra("tipoHabitacion") ?: ""
        precioPorNoche = intent.getDoubleExtra("precioPorNoche", 0.0)
        precioTotal = intent.getDoubleExtra("precioTotal", 0.0)
        adultos = intent.getIntExtra("adultos", 1)
        ninos = intent.getIntExtra("ninos", 0)
        fechaEntrada = intent.getStringExtra("fechaEntrada") ?: ""
        fechaSalida = intent.getStringExtra("fechaSalida") ?: ""
        numNoches = intent.getIntExtra("numNoches", 1)
        destino = intent.getStringExtra("destino") ?: ""

        // Generar campos para cada adulto y niño
        generarCamposPersonas()

        // Configurar listener del botón
        btnConfirmarReserva.setOnClickListener {
            if (validarFormulario()) {
                // Navegar a ConfirmacionHotelesActivity
                navegarAConfirmacion()
            }
        }
    }

    private fun generarCamposPersonas() {
        // Limpiar el contenedor y las listas
        containerPersonas.removeAllViews()
        personasNombre.clear()
        personasApellidos.clear()
        personasDni.clear()
        personasEdad.clear()
        personasEmail.clear()

        // Agregar campos para adultos
        for (i in 1..adultos) {
            agregarPersona("Adulto $i", true)
        }

        // Agregar campos para niños
        for (i in 1..ninos) {
            agregarPersona("Niño $i", false)
        }
    }

    private fun agregarPersona(titulo: String, esAdulto: Boolean) {
        val inflater = LayoutInflater.from(this)
        val personaView = inflater.inflate(R.layout.layout_persona_item, containerPersonas, false)

        // Obtener referencias a los campos
        val tvTituloPersona = personaView.findViewById<TextView>(R.id.tvTituloPersona)
        val etNombre = personaView.findViewById<TextInputEditText>(R.id.etNombre)
        val etApellidos = personaView.findViewById<TextInputEditText>(R.id.etApellidos)
        val etDni = personaView.findViewById<TextInputEditText>(R.id.etDni)
        val etEdad = personaView.findViewById<TextInputEditText>(R.id.etEdad)
        val etEmail = personaView.findViewById<TextInputEditText>(R.id.etEmail)

        // Establecer título
        tvTituloPersona.text = titulo

        // Establecer hint para la edad según si es adulto o niño
        if (esAdulto) {
            etEdad.hint = "Edad (18 años o más)"
        } else {
            etEdad.hint = "Edad (menor de 18 años)"
        }

        // Añadir a las listas para validación posterior
        personasNombre.add(etNombre)
        personasApellidos.add(etApellidos)
        personasDni.add(etDni)
        personasEdad.add(etEdad)
        personasEmail.add(etEmail)

        // Añadir vista al contenedor
        containerPersonas.addView(personaView)
    }

    private fun validarFormulario(): Boolean {
        var isValid = true

        // Validar cada persona
        for (i in 0 until (adultos + ninos)) {
            val esAdulto = i < adultos // Los primeros son adultos

            val nombre = personasNombre[i].text.toString().trim()
            if (nombre.isEmpty()) {
                personasNombre[i].error = "El nombre es obligatorio"
                isValid = false
            } else {
                personasNombre[i].error = null
            }

            val apellidos = personasApellidos[i].text.toString().trim()
            if (apellidos.isEmpty()) {
                personasApellidos[i].error = "Los apellidos son obligatorios"
                isValid = false
            } else {
                personasApellidos[i].error = null
            }

            val dni = personasDni[i].text.toString().trim()
            if (dni.isEmpty()) {
                personasDni[i].error = "El DNI es obligatorio"
                isValid = false
            } else if (!validarDNI(dni)) {
                personasDni[i].error = "DNI no válido"
                isValid = false
            } else {
                personasDni[i].error = null
            }

            val edadStr = personasEdad[i].text.toString().trim()
            if (edadStr.isEmpty()) {
                personasEdad[i].error = "La edad es obligatoria"
                isValid = false
            } else {
                try {
                    val edad = edadStr.toInt()
                    if (edad <= 0 || edad > 120) {
                        personasEdad[i].error = "Edad no válida"
                        isValid = false
                    } else if (esAdulto && edad < 18) {
                        personasEdad[i].error = "Los adultos deben ser mayores de 18 años"
                        isValid = false
                    } else if (!esAdulto && edad >= 18) {
                        personasEdad[i].error = "Los niños deben ser menores de 18 años"
                        isValid = false
                    } else {
                        personasEdad[i].error = null
                    }
                } catch (e: NumberFormatException) {
                    personasEdad[i].error = "Ingrese una edad válida"
                    isValid = false
                }
            }

            val email = personasEmail[i].text.toString().trim()
            if (email.isEmpty()) {
                personasEmail[i].error = "El email es obligatorio"
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                personasEmail[i].error = "Email no válido"
                isValid = false
            } else {
                personasEmail[i].error = null
            }
        }

        if (!isValid) {
            Toast.makeText(this, "Por favor, corrija los errores en el formulario", Toast.LENGTH_LONG).show()
        }

        return isValid
    }

    private fun navegarAConfirmacion() {
        hotel?.let { hotelData ->
            val intent = Intent(this, ConfirmacionHotelesActivity::class.java).apply {
                putExtra("hotel", hotelData)
                putExtra("adultos", adultos)
                putExtra("ninos", ninos)
                putExtra("totalPrice", precioTotal)
                putExtra("fechaEntrada", fechaEntrada)
                putExtra("fechaSalida", fechaSalida)
                putExtra("destino", destino)
                putExtra("tipoHabitacion", tipoHabitacion)
                putExtra("noches", numNoches)
            }
            startActivity(intent)
            finish() // Opcional: cerrar esta actividad
        } ?: run {
            Toast.makeText(this, "Error: No se encontraron los datos del hotel", Toast.LENGTH_LONG).show()
        }
    }

    // Función auxiliar para validar formato de DNI español
    private fun validarDNI(dni: String): Boolean {
        // Patrón básico: 8 números seguidos de una letra
        val patron = Regex("^\\d{8}[A-Za-z]$")
        if (!patron.matches(dni)) {
            return false
        }

        // Validación más completa (opcional)
        val letras = "TRWAGMYFPDXBNJZSQVHLCKE"
        val numero = dni.substring(0, 8).toInt()
        val letra = dni[8].uppercaseChar()
        val letraCalculada = letras[numero % 23]

        return letra == letraCalculada
    }
}