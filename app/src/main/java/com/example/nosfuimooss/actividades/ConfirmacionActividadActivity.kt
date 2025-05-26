package com.example.nosfuimooss.actividades

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
import com.example.nosfuimooss.model.Actividad
import com.example.nosfuimooss.navegador.MisViajesActivity
import com.example.nosfuimooss.sesion.LoginActivity
import com.example.nosfuimooss.usuario.ReservaActividad
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Locale

class ConfirmacionActividadActivity : AppCompatActivity() {
    private lateinit var etEmailUsuario: TextInputEditText
    private lateinit var etPasswordUsuario: TextInputEditText
    private lateinit var tilEmailUsuario: TextInputLayout
    private lateinit var tilPasswordUsuario: TextInputLayout
    private lateinit var btnConfirmarReserva: Button
    private lateinit var btnCancelarConfirmacion: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Datos de la actividad
    private lateinit var actividad: Actividad
    private var fechaSeleccionada: String = ""
    private var numeroParticipantes: Int = 1
    private var precioTotal: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmacion_actividad)
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

        // Configurar información resumida de la actividad
        setupResumenActividad()

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
        actividad = intent.getSerializableExtra("actividad") as Actividad
        fechaSeleccionada = intent.getStringExtra("fechaSeleccionada") ?: ""
        numeroParticipantes = intent.getIntExtra("numeroParticipantes", 1)
        precioTotal = intent.getDoubleExtra("precioTotal", 0.0)

        Log.d("ConfirmacionActividades", "Actividad: ${actividad.nombre}")
        Log.d("ConfirmacionActividades", "Fecha: $fechaSeleccionada")
        Log.d("ConfirmacionActividades", "Participantes: $numeroParticipantes")
        Log.d("ConfirmacionActividades", "Precio total: $precioTotal")
    }

    private fun setupResumenActividad() {
        // Configurar encabezado
        val tvNombreActividad = findViewById<TextView>(R.id.tvNombreActividad)
        val tvPrecioConfirmacion = findViewById<TextView>(R.id.tvPrecioConfirmacion)

        tvNombreActividad.text = actividad.nombre ?: "Actividad"
        tvPrecioConfirmacion.text = "Total: ${String.format("%.2f", precioTotal)} €"

        // Configurar tarjeta de resumen
        val tvResumenActividad = findViewById<TextView>(R.id.tvResumenActividad)
        val tvResumenFecha = findViewById<TextView>(R.id.tvResumenFecha)
        val tvResumenUbicacion = findViewById<TextView>(R.id.tvResumenUbicacion)
        val tvResumenParticipantes = findViewById<TextView>(R.id.tvResumenParticipantes)
        val tvResumenDuracion = findViewById<TextView>(R.id.tvResumenDuracion)
        val tvResumenEstrellas = findViewById<TextView>(R.id.tvResumenEstrellas)

        // Configurar cada campo
        tvResumenActividad.text = actividad.nombre ?: "Reserva de actividad"

        // Formatear fecha
        if (fechaSeleccionada.isNotEmpty()) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val date = inputFormat.parse(fechaSeleccionada)
                tvResumenFecha.text = "Fecha: ${date?.let { outputFormat.format(it) } ?: fechaSeleccionada}"
            } catch (e: Exception) {
                tvResumenFecha.text = "Fecha: $fechaSeleccionada"
            }
        } else {
            tvResumenFecha.text = "Fecha: No especificada"
        }

        tvResumenUbicacion.text = "Ubicación: ${actividad.ubicacion ?: "No especificada"}"

        tvResumenParticipantes.text = if (numeroParticipantes == 1) {
            "Participantes: 1 persona"
        } else {
            "Participantes: $numeroParticipantes personas"
        }

        tvResumenDuracion.text = "Duración: ${actividad.tiempo ?: "No especificada"}"

        // Mostrar estrellas
        val estrellas = actividad.estrellas
        val estrellasText = "⭐".repeat(estrellas.toInt()) +
                if (estrellas % 1 != 0.0) "⭐" else "" // Media estrella si hay decimales
        tvResumenEstrellas.text = "Calificación: $estrellasText ($estrellas)"
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
        Log.d("ConfirmacionActividades", "Guardando reserva de actividad para usuario: $userId")

        // Crear ID único para la reserva
        val reservaId = database.reference.child("reservas_actividades").child(userId).push().key ?: return
        val fechaReserva = System.currentTimeMillis()

        // Crear objeto de reserva para actividad
        val reservaActividad = mapOf(
            "id" to reservaId,
            "userId" to userId,
            "actividad" to mapOf(
                "id" to (actividad.id ?: ""),
                "nombre" to (actividad.nombre ?: ""),
                "ubicacion" to (actividad.ubicacion ?: ""),
                "precio" to actividad.precio,
                "estrellas" to actividad.estrellas,
                "tiempo" to (actividad.tiempo ?: ""),
                "foto" to (actividad.foto ?: ""),
                "resumen" to (actividad.resumen ?: ""),
                "queEstaIncluido" to (actividad.queEstaIncluido ?: ""),
                "puntoRecogida" to (actividad.puntoRecogida ?: "")
            ),
            "fechaSeleccionada" to fechaSeleccionada,
            "numeroParticipantes" to numeroParticipantes,
            "precioTotal" to precioTotal,
            "fechaReserva" to fechaReserva,
            "estado" to "Confirmada"
        )

        // Guardar en Firebase
        database.reference
            .child("reservas_actividades")
            .child(userId)
            .child(reservaId)
            .setValue(reservaActividad)
            .addOnCompleteListener { task ->
                loadingDialog.dismiss()

                if (task.isSuccessful) {
                    Log.d("ConfirmacionActividades", "Reserva de actividad guardada con éxito: $reservaId")
                    mostrarConfirmacionExitosa(reservaId)
                } else {
                    Log.e("ConfirmacionActividades", "Error al guardar reserva: ${task.exception?.message}")
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
            .setTitle("¡Reserva confirmada!")
            .setMessage("Tu reserva de actividad se ha realizado con éxito. El código de tu reserva es: $reservaId")
            .setPositiveButton("Ver mis reservas") { _, _ ->
                val intent = Intent(this, MisViajesActivity::class.java)
                intent.putExtra("nuevaReservaActividadId", reservaId)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Volver a actividades") { _, _ ->
                val intent = Intent(this, DetalleActividadActivity::class.java)
                intent.putExtra("actividad", actividad)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)

        dialog.show()
    }
}