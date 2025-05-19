package com.example.nosfuimooss.boleto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Boleto
import com.example.nosfuimooss.model.Pasajero
import com.example.nosfuimooss.model.PreferenciasUsuario
import com.google.android.material.textfield.TextInputEditText

class DatosPasajerosActivity : AppCompatActivity() {
    private lateinit var boletoIda: Boleto
    private lateinit var boletoVuelta: Boleto
    private lateinit var prefs: PreferenciasUsuario
    private var isRoundTrip = false
    private var totalPrice = 0.0
    private var fareType = ""
    private var passengerCount = 1
    private var idaDeparture = ""
    private var idaArrival = ""
    private var vueltaDeparture = ""
    private var vueltaArrival = ""
    private var fechaIda: Long = 0L
    private var fechaVuelta: Long = 0L
    private var imageUrl = ""

    private lateinit var layoutPasajeros: LinearLayout
    private lateinit var btnComprar: Button
    private val pasajeros = mutableListOf<Pasajero>()
    private val formulariosPasajeros = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_pasajeros)
        supportActionBar?.hide()

        // Recuperar los extras del intent
        boletoIda = intent.getSerializableExtra("boletoIda") as Boleto
        boletoVuelta = intent.getSerializableExtra("boletoVuelta") as? Boleto ?: boletoIda
        prefs = intent.getSerializableExtra("preferencias") as PreferenciasUsuario
        isRoundTrip = intent.getBooleanExtra("isRoundTrip", false)
        totalPrice = intent.getDoubleExtra("totalPrice", 0.0)
        fareType = intent.getStringExtra("fareType") ?: ""
        passengerCount = prefs.cantidadPersonas
        idaDeparture = intent.getStringExtra("idaDepartureTime") ?: ""
        idaArrival = intent.getStringExtra("idaArrivalTime") ?: ""
        fechaIda = intent.getLongExtra("fechaIda", 0L)
        imageUrl = intent.getStringExtra("imageUrl") ?: ""

        // Asegúrate de obtener estos valores para ida y vuelta
        if (isRoundTrip) {
            vueltaDeparture = intent.getStringExtra("vueltaDepartureTime") ?: ""
            vueltaArrival = intent.getStringExtra("vueltaArrivalTime") ?: ""
            fechaVuelta = intent.getLongExtra("fechaVuelta", 0L)

            // Agregar log para verificar
            Log.d("DatosPasajeros", "Recibido fechaVuelta: $fechaVuelta")

            // Si la fecha de vuelta es 0, intentar obtenerla de las preferencias
            if (fechaVuelta == 0L && prefs.fechaFin > 0) {
                fechaVuelta = prefs.fechaFin
                Log.d("DatosPasajeros", "Usando fechaFin como respaldo: $fechaVuelta")
            }
        }
        // Inicializar vistas
        layoutPasajeros = findViewById(R.id.layoutPasajeros)
        btnComprar = findViewById(R.id.btnComprar)

        // Mostrar información de la ruta y precio
        val tvRutaViaje = findViewById<TextView>(R.id.tvRutaViaje)
        val tvPrecioTotal = findViewById<TextView>(R.id.tvPrecioTotal)

        tvRutaViaje.text = if (isRoundTrip) {
            "${boletoIda.origen} ↔ ${boletoIda.destino}"
        } else {
            "${boletoIda.origen} → ${boletoIda.destino}"
        }

        tvPrecioTotal.text = "Precio total: ${totalPrice.toInt()} €"

        // Crear formularios para cada pasajero
        for (i in 1..passengerCount) {
            pasajeros.add(Pasajero())
            createPassengerForm(i)
        }

        // Configurar botón de compra
        btnComprar.setOnClickListener {
            if (validatePassengersData()) {
                goToConfirmacionReserva()
            }
        }
    }

    private fun createPassengerForm(index: Int) {
        val formView = LayoutInflater.from(this).inflate(R.layout.layout_pasajero, layoutPasajeros, false)

        // Configurar el título del pasajero
        val tvTituloPasajero = formView.findViewById<TextView>(R.id.tvTituloPasajero)
        tvTituloPasajero.text = "Datos del pasajero $index"

        // Configurar dropdown de género
        val actvGenero = formView.findViewById<AutoCompleteTextView>(R.id.actvGenero)
        val generos = arrayOf("Masculino", "Femenino", "No especificar")
        val adapterGenero = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, generos)
        actvGenero.setAdapter(adapterGenero)

        // Añadir el formulario a la vista y guardarlo en la lista
        layoutPasajeros.addView(formView)
        formulariosPasajeros.add(formView)
    }

    private fun validatePassengersData(): Boolean {
        for (i in formulariosPasajeros.indices) {
            val formView = formulariosPasajeros[i]

            val etNombre = formView.findViewById<TextInputEditText>(R.id.etNombre)
            val etApellidos = formView.findViewById<TextInputEditText>(R.id.etApellidos)
            val etEmail = formView.findViewById<TextInputEditText>(R.id.etEmail)
            val etDni = formView.findViewById<TextInputEditText>(R.id.etDni)
            val etEdad = formView.findViewById<TextInputEditText>(R.id.etEdad)
            val actvGenero = formView.findViewById<AutoCompleteTextView>(R.id.actvGenero)

            val nombre = etNombre.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val dni = etDni.text.toString().trim()
            val edadStr = etEdad.text.toString().trim()
            val genero = actvGenero.text.toString().trim()

            // Validar que todos los campos estén completos
            if (nombre.isEmpty() || apellidos.isEmpty() || email.isEmpty() ||
                dni.isEmpty() || edadStr.isEmpty() || genero.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los datos del pasajero ${i+1}", Toast.LENGTH_SHORT).show()
                return false
            }

            // Validar formato de email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email del pasajero ${i+1} no válido", Toast.LENGTH_SHORT).show()
                return false
            }

            // Validar DNI (formato simplificado)
            if (!validarDni(dni)) {
                Toast.makeText(this, "DNI del pasajero ${i+1} no válido", Toast.LENGTH_SHORT).show()
                return false
            }

            // Validar edad
            val edad = edadStr.toIntOrNull() ?: 0

            // Si solo hay un pasajero o es el primer pasajero, debe ser mayor de 18
            if ((passengerCount == 1 || i == 0) && edad < 18) {
                Toast.makeText(this, "El pasajero principal debe ser mayor de edad", Toast.LENGTH_SHORT).show()
                return false
            }

            // Guardar los datos validados en el objeto Pasajero
            pasajeros[i] = Pasajero(
                nombre = nombre,
                apellidos = apellidos,
                email = email,
                dni = dni,
                edad = edad,
                genero = genero
            )
        }

        return true
    }

    private fun validarDni(dni: String): Boolean {
        // Validación simple: 8 números seguidos de una letra
        val patron = Regex("^[0-9]{8}[A-Za-z]$")
        return patron.matches(dni)
    }

    private fun goToConfirmacionReserva() {
        Intent(this, ConfirmacionReservaActivity::class.java).apply {
            putExtra("boletoIda", boletoIda)
            putExtra("boletoVuelta", boletoVuelta)
            putExtra("preferencias", prefs)
            putExtra("isRoundTrip", isRoundTrip)
            putExtra("totalPrice", totalPrice)
            putExtra("fareType", fareType)
            putExtra("imageUrl", imageUrl)

            // Pasar datos de tiempo y fechas
            putExtra("idaDepartureTime", idaDeparture)
            putExtra("idaArrivalTime", idaArrival)
            putExtra("fechaIda", fechaIda)

            if (isRoundTrip) {
                putExtra("vueltaDepartureTime", vueltaDeparture)
                putExtra("vueltaArrivalTime", vueltaArrival)
                putExtra("fechaVuelta", fechaVuelta) // Asegúrate de que esto tenga un valor

                // Log para verificar
                Log.d("DatosPasajeros", "Enviando fechaVuelta a ConfirmacionReserva: $fechaVuelta")
            }

            // Pasar la lista de pasajeros como Serializable
            putExtra("pasajeros", ArrayList(pasajeros))
        }.also { startActivity(it) }
    }
}