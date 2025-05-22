package com.example.nosfuimooss.hotel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Hotel
import com.example.nosfuimooss.navegador.MisViajesActivity
import com.example.nosfuimooss.sesion.LoginActivity
import com.example.nosfuimooss.usuario.ReservaHotel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ConfirmacionHotelesActivity : AppCompatActivity() {
    private lateinit var etEmailUsuario: TextInputEditText
    private lateinit var etPasswordUsuario: TextInputEditText
    private lateinit var tilEmailUsuario: TextInputLayout
    private lateinit var tilPasswordUsuario: TextInputLayout
    private lateinit var btnConfirmarReserva: Button
    private lateinit var btnCancelarConfirmacion: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Datos de reserva de hotel
    private lateinit var hotel: Hotel
    private var adultos = 1
    private var ninos = 0
    private var totalPrice = 0.0
    private var fechaEntrada = ""
    private var fechaSalida = ""
    private var destino = ""
    private var tipoHabitacion = ""
    private var noches = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmacion_hoteles)
        supportActionBar?.hide()

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Verificar si el usuario está autenticado
        if (auth.currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para completar la reserva", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Inicializar vistas
        initViews()

        // Recuperar datos del intent
        getIntentData()

        // Configurar información resumida del hotel
        setupResumenHotel()

        // Configurar listeners de botones
        setupButtonListeners()
    }

    private fun initViews() {
        etEmailUsuario = findViewById(R.id.etEmailUsuario)
        etPasswordUsuario = findViewById(R.id.etPasswordUsuario)
        tilEmailUsuario = findViewById(R.id.tilEmailUsuario)
        tilPasswordUsuario = findViewById(R.id.tilPasswordUsuario)
        btnConfirmarReserva = findViewById(R.id.btnConfirmarReserva)
        btnCancelarConfirmacion = findViewById(R.id.btnCancelarConfirmacion)

        // Pre-rellenar el correo electrónico del usuario actual y bloquearlo
        auth.currentUser?.email?.let { userEmail ->
            etEmailUsuario.setText(userEmail)
            etEmailUsuario.isEnabled = false
            etEmailUsuario.setTextColor(resources.getColor(R.color.disabled_text, theme))
        }
    }

    private fun getIntentData() {
        hotel = intent.getSerializableExtra("hotel") as Hotel
        adultos = intent.getIntExtra("adultos", 1)
        ninos = intent.getIntExtra("ninos", 0)
        totalPrice = intent.getDoubleExtra("totalPrice", 0.0)
        fechaEntrada = intent.getStringExtra("fechaEntrada") ?: ""
        fechaSalida = intent.getStringExtra("fechaSalida") ?: ""
        destino = intent.getStringExtra("destino") ?: ""
        tipoHabitacion = intent.getStringExtra("tipoHabitacion") ?: "Estándar"
        noches = intent.getIntExtra("noches", 1)

        Log.d("ConfirmacionHoteles", "Hotel: ${hotel.nombre}")
        Log.d("ConfirmacionHoteles", "Adultos: $adultos, Niños: $ninos")
        Log.d("ConfirmacionHoteles", "Precio total: $totalPrice")
        Log.d("ConfirmacionHoteles", "Fechas: $fechaEntrada - $fechaSalida")
    }

    private fun setupResumenHotel() {
        // Configurar encabezado
        val tvRutaConfirmacion = findViewById<TextView>(R.id.tvRutaConfirmacion)
        val tvPrecioConfirmacion = findViewById<TextView>(R.id.tvPrecioConfirmacion)

        tvRutaConfirmacion.text = "${hotel.nombre} - ${hotel.ubicacion}"
        tvPrecioConfirmacion.text = "Total: ${totalPrice.toInt()} €"

        // Configurar tarjeta de resumen
        val tvResumenViaje = findViewById<TextView>(R.id.tvResumenViaje)
        val tvResumenFechas = findViewById<TextView>(R.id.tvResumenFechas)
        val tvResumenHorarios = findViewById<TextView>(R.id.tvResumenHorarios)
        val tvResumenPasajeros = findViewById<TextView>(R.id.tvResumenPasajeros)
        val tvResumenTarifa = findViewById<TextView>(R.id.tvResumenTarifa)
        val tvResumenClase = findViewById<TextView>(R.id.tvResumenClase)

        // Tipo de reserva
        tvResumenViaje.text = "Reserva de hotel"

        // Fechas
        val fechasText = "Entrada: $fechaEntrada\nSalida: $fechaSalida"
        tvResumenFechas.text = fechasText

        // Información del hotel (usando el campo horarios para mostrar dirección)
        val direccionText = hotel.direccion ?: "Dirección no disponible"
        tvResumenHorarios.text = "Dirección: $direccionText"

        // Huéspedes
        val totalPersonas = adultos + ninos
        val huespedesText = if (ninos > 0) {
            "Huéspedes: $adultos adultos, $ninos niños ($totalPersonas total)"
        } else {
            "Huéspedes: $adultos adultos"
        }
        tvResumenPasajeros.text = huespedesText

        // Tipo de habitación y noches
        tvResumenTarifa.text = "Habitación: $tipoHabitacion"
        tvResumenClase.text = "Noches: $noches (${hotel.estrellas} estrellas)"
    }

    private fun setupButtonListeners() {
        btnCancelarConfirmacion.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("¿Cancelar reserva?")
                .setMessage("¿Estás seguro de que deseas cancelar tu reserva? Perderás todos los datos ingresados.")
                .setPositiveButton("Sí, cancelar") { _, _ ->
                    finish()
                }
                .setNegativeButton("No, continuar", null)
                .show()
        }

        btnConfirmarReserva.setOnClickListener {
            verificarUsuarioYGuardarReserva()
        }
    }

    private fun verificarUsuarioYGuardarReserva() {
        val email = etEmailUsuario.text.toString().trim()
        val password = etPasswordUsuario.text.toString()

        // Limpiar errores previos
        tilEmailUsuario.error = null
        tilPasswordUsuario.error = null

        // Validar campos
        var isValid = true

        if (email.isEmpty()) {
            tilEmailUsuario.error = "Ingresa tu correo electrónico"
            isValid = false
        }

        if (password.isEmpty()) {
            tilPasswordUsuario.error = "Ingresa tu contraseña"
            isValid = false
        }

        if (!isValid) return

        val userEmail = auth.currentUser?.email
        if (userEmail == null) {
            Toast.makeText(this, "Error de autenticación. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Mostrar progreso
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()

        loadingDialog.show()

        // Reautenticar al usuario
        val credential = EmailAuthProvider.getCredential(userEmail, password)
        auth.currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    guardarReservaEnFirebase(loadingDialog)
                } else {
                    loadingDialog.dismiss()
                    tilPasswordUsuario.error = "Contraseña incorrecta"
                }
            }
    }

    private fun guardarReservaEnFirebase(loadingDialog: AlertDialog) {
        val userId = auth.currentUser?.uid ?: return
        Log.d("ConfirmacionHoteles", "Guardando reserva de hotel para usuario: $userId")

        // Crear ID de reserva
        val reservaId = database.reference.child("reservasHoteles").child(userId).push().key ?: return
        Log.d("ConfirmacionHoteles", "Reserva Hotel ID generado: $reservaId")
        val fechaReserva = System.currentTimeMillis()

        // Crear objeto ReservaHotel
        val reservaHotel = ReservaHotel(
            id = reservaId,
            userId = userId,
            hotel = hotel,
            fechaEntrada = fechaEntrada,
            fechaSalida = fechaSalida,
            adultos = adultos,
            ninos = ninos,
            tipoHabitacion = tipoHabitacion,
            noches = noches,
            precio = totalPrice,
            fechaReserva = fechaReserva,
            estado = "Confirmada",
            destino = destino
        )

        // Guardar en la base de datos
        database.reference.child("reservasHoteles").child(userId).child(reservaId).setValue(reservaHotel)
            .addOnCompleteListener { task ->
                loadingDialog.dismiss()

                if (task.isSuccessful) {
                    Log.d("ConfirmacionHoteles", "Reserva de hotel guardada con éxito: $reservaId")
                    mostrarConfirmacionExitosa(reservaId)
                } else {
                    Log.e("ConfirmacionHoteles", "Error al guardar reserva de hotel: ${task.exception?.message}")
                    Toast.makeText(
                        this,
                        "Error al guardar la reserva: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun mostrarConfirmacionExitosa(reservaId: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("¡Reserva de hotel confirmada!")
            .setMessage("Tu reserva de hotel se ha realizado con éxito.\n\nHotel: ${hotel.nombre}\nCódigo de reserva: $reservaId")
            .setPositiveButton("Ver mis reservas") { _, _ ->
                val intent = Intent(this, MisViajesActivity::class.java)
                intent.putExtra("tab_position", 1) // 1 para la pestaña de Hoteles
                intent.putExtra("nuevaReservaHotelId", reservaId)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .setCancelable(false)

        dialog.setNegativeButton("Volver a hoteles") { _, _ ->
            // Volver a la actividad de hoteles del destino
            val intent = Intent(this, ReservarHotel::class.java)
            intent.putExtra("destino", destino)
            startActivity(intent)
            finish()
        }

        dialog.show()
    }
}