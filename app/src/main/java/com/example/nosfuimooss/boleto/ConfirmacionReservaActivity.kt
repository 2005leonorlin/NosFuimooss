package com.example.nosfuimooss.boleto


import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Boleto
import com.example.nosfuimooss.model.Pasajero
import com.example.nosfuimooss.model.PreferenciasUsuario
import com.example.nosfuimooss.model.Reserva
import com.example.nosfuimooss.navegador.MisViajesActivity
import com.example.nosfuimooss.sesion.LoginActivity
import com.example.nosfuimooss.usuariologeado.DetalleVuelos
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ConfirmacionReservaActivity : AppCompatActivity() {
    private lateinit var etEmailUsuario: TextInputEditText
    private lateinit var etPasswordUsuario: TextInputEditText
    private lateinit var tilEmailUsuario: TextInputLayout
    private lateinit var tilPasswordUsuario: TextInputLayout
    private lateinit var btnConfirmarReserva: Button
    private lateinit var btnCancelarConfirmacion: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Datos de reserva
    private lateinit var boletoIda: Boleto
    private var boletoVuelta: Boleto? = null
    private lateinit var prefs: PreferenciasUsuario
    private var roundTrip = false
    private var totalPrice = 0.0
    private var fareType = ""
    private var imageUrl = ""
    private var idaDeparture = ""
    private var idaArrival = ""
    private var vueltaDeparture = ""
    private var vueltaArrival = ""
    private var fechaIda: Long = 0L
    private var fechaVuelta: Long = 0L
    private lateinit var pasajeros: ArrayList<Pasajero>
    private var destinoId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmacion_reserva)
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

        // Configurar información resumida del viaje
        setupResumenViaje()

        // Configurar listeners de botones
        setupButtonListeners()
    }

    private fun initViews() {
        etEmailUsuario = findViewById(R.id.etEmailUsuario)
        etPasswordUsuario = findViewById(R.id.etPasswordUsuario)

        // Añadir TextInputLayout para mejor manejo de errores
        tilEmailUsuario = findViewById(R.id.tilEmailUsuario)
        tilPasswordUsuario = findViewById(R.id.tilPasswordUsuario)

        btnConfirmarReserva = findViewById(R.id.btnConfirmarReserva)
        btnCancelarConfirmacion = findViewById(R.id.btnCancelarConfirmacion)

        // Pre-rellenar el correo electrónico del usuario actual y bloquearlo
        auth.currentUser?.email?.let { userEmail ->
            etEmailUsuario.setText(userEmail)
            etEmailUsuario.isEnabled = false  // Bloquea la edición del email
            etEmailUsuario.setTextColor(resources.getColor(R.color.disabled_text, theme))
        }
    }

    private fun getIntentData() {
        boletoIda = intent.getSerializableExtra("boletoIda") as Boleto

        // Cambio importante: Manejamos correctamente el boleto de vuelta
        roundTrip = intent.getBooleanExtra("isRoundTrip", false)
        if (roundTrip) {
            boletoVuelta = intent.getSerializableExtra("boletoVuelta") as? Boleto
            if (boletoVuelta == null) {
                Log.e("ConfirmacionReserva", "Error: isRoundTrip es true pero boletoVuelta es null")
                // Podemos usar boletoIda como respaldo para no tener null
                boletoVuelta = boletoIda
            }
        }

        prefs = intent.getSerializableExtra("preferencias") as PreferenciasUsuario
        totalPrice = intent.getDoubleExtra("totalPrice", 0.0)
        fareType = intent.getStringExtra("fareType") ?: ""
        imageUrl = intent.getStringExtra("imageUrl") ?: ""
        destinoId = intent.getStringExtra("destinoId") ?: ""

        idaDeparture = intent.getStringExtra("idaDepartureTime") ?: ""
        idaArrival = intent.getStringExtra("idaArrivalTime") ?: ""
        fechaIda = intent.getLongExtra("fechaIda", 0L)

        // Agregar más logs para verificar los valores recibidos
        Log.d("ConfirmacionReserva", "isRoundTrip: $roundTrip")
        Log.d("ConfirmacionReserva", "fechaIda: $fechaIda")

        if (roundTrip) {
            vueltaDeparture = intent.getStringExtra("vueltaDepartureTime") ?: ""
            vueltaArrival = intent.getStringExtra("vueltaArrivalTime") ?: ""
            fechaVuelta = intent.getLongExtra("fechaVuelta", 0L)

            // Si la fecha de vuelta es 0, intentar obtenerla desde preferencias
            if (fechaVuelta == 0L && prefs.fechaFin > 0) {
                fechaVuelta = prefs.fechaFin
                Log.d("ConfirmacionReserva", "Usando fechaFin como respaldo: $fechaVuelta")
            }

            // Debug logs para verificar datos
            Log.d("ConfirmacionReserva", "fechaVuelta: $fechaVuelta")
            Log.d("ConfirmacionReserva", "Fecha vuelta formateada: " +
                    if (fechaVuelta > 0) {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(fechaVuelta))
                    } else {
                        "Fecha inválida"
                    }
            )
        }

        @Suppress("UNCHECKED_CAST")
        pasajeros = intent.getSerializableExtra("pasajeros") as ArrayList<Pasajero>
    }

    private fun setupResumenViaje() {
        // Configurar encabezado
        val tvRutaConfirmacion = findViewById<TextView>(R.id.tvRutaConfirmacion)
        val tvPrecioConfirmacion = findViewById<TextView>(R.id.tvPrecioConfirmacion)

        tvRutaConfirmacion.text = if (roundTrip) {
            "${boletoIda.origen} ↔ ${boletoIda.destino}"
        } else {
            "${boletoIda.origen} → ${boletoIda.destino}"
        }

        tvPrecioConfirmacion.text = "Total: ${totalPrice.toInt()} €"

        // Configurar tarjeta de resumen
        val tvResumenViaje = findViewById<TextView>(R.id.tvResumenViaje)
        val tvResumenFechas = findViewById<TextView>(R.id.tvResumenFechas)
        val tvResumenHorarios = findViewById<TextView>(R.id.tvResumenHorarios)
        val tvResumenPasajeros = findViewById<TextView>(R.id.tvResumenPasajeros)
        val tvResumenTarifa = findViewById<TextView>(R.id.tvResumenTarifa)
        val tvResumenClase = findViewById<TextView>(R.id.tvResumenClase)

        // Tipo de viaje
        tvResumenViaje.text = if (roundTrip) "Viaje de ida y vuelta" else "Viaje solo ida"

        // Fechas formateadas
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val fechaIdaStr = if (fechaIda > 0) {
            fmt.format(Date(fechaIda))
        } else {
            "Fecha no disponible"
        }

        // Ahora comprobamos con más detalle la fecha de vuelta
        val fechaVueltaStr = if (roundTrip && fechaVuelta > 0) {
            fmt.format(Date(fechaVuelta))
        } else if (roundTrip) {
            "Fecha de vuelta no disponible"
        } else {
            ""
        }

        val fechasText = if (roundTrip) {
            "Fechas: $fechaIdaStr - $fechaVueltaStr"
        } else {
            "Fecha: $fechaIdaStr"
        }
        tvResumenFechas.text = fechasText

        // Horarios - Verificamos también si los horarios tienen valores válidos
        val idaDepartureText = idaDeparture.ifEmpty { "No disponible" }
        val idaArrivalText = idaArrival.ifEmpty { "No disponible" }
        val vueltaDepartureText = if (roundTrip) vueltaDeparture.ifEmpty { "No disponible" } else ""
        val vueltaArrivalText = if (roundTrip) vueltaArrival.ifEmpty { "No disponible" } else ""

        val horariosText = if (roundTrip) {
            "Ida: $idaDepartureText → $idaArrivalText\nVuelta: $vueltaDepartureText → $vueltaArrivalText"
        } else {
            "Horario: $idaDepartureText → $idaArrivalText"
        }
        tvResumenHorarios.text = horariosText

        // Pasajeros
        tvResumenPasajeros.text = "Pasajeros: ${pasajeros.size}"

        // Tarifa y clase
        tvResumenTarifa.text = "Tarifa: $fareType"
        tvResumenClase.text = "Clase: ${prefs.categoria}"
    }

    private fun setupButtonListeners() {
        btnCancelarConfirmacion.setOnClickListener {
            // Mostrar diálogo de confirmación antes de cancelar
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

        // No necesitamos validar que el email coincida ya que ahora lo bloqueamos
        // y usamos directamente el email del usuario autenticado
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

        // Reautenticar al usuario para verificar sus credenciales
        val credential = EmailAuthProvider.getCredential(userEmail, password)
        auth.currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Guardar la reserva en la base de datos
                    guardarReservaEnFirebase(loadingDialog)
                } else {
                    loadingDialog.dismiss()
                    // Mostrar error específico en el campo de contraseña
                    tilPasswordUsuario.error = "Contraseña incorrecta"
                }
            }
    }

    private fun guardarReservaEnFirebase(loadingDialog: AlertDialog) {
        val userId = auth.currentUser?.uid ?: return
        Log.d("ConfirmacionReserva", "Guardando reserva para usuario: $userId")

        // Crear objeto Reserva
        val reservaId = database.reference.child("reservas").child(userId).push().key ?: return
        Log.d("ConfirmacionReserva", "Reserva ID generado: $reservaId")
        val fechaReserva = System.currentTimeMillis()

        // Crear correctamente el objeto Reserva con todos los datos necesarios
        val reserva = Reserva(
            id = reservaId,
            userId = userId,
            boletoIda = boletoIda,
            boletoVuelta = if (roundTrip) boletoVuelta else null,
            fechaIda = fechaIda,
            fechaVuelta = if (roundTrip) fechaVuelta else 0,
            idaDepartureTime = idaDeparture,
            idaArrivalTime = idaArrival,
            vueltaDepartureTime = if (roundTrip) vueltaDeparture else "",
            vueltaArrivalTime = if (roundTrip) vueltaArrival else "",
            pasajeros = pasajeros,
            roundTrip = roundTrip,
            precio = totalPrice,
            fechaReserva = fechaReserva,
            estado = "Confirmada",
            tarifa = fareType,
            clase = prefs.categoria,
            imageUrl = imageUrl
        )

        // Guardar en la base de datos
        database.reference.child("reservas").child(userId).child(reservaId).setValue(reserva)
            .addOnCompleteListener { task ->
                loadingDialog.dismiss()

                if (task.isSuccessful) {
                    Log.d("ConfirmacionReserva", "Reserva guardada con éxito: $reservaId")
                    mostrarConfirmacionExitosa(reservaId)
                } else {
                    Log.e("ConfirmacionReserva", "Error al guardar reserva: ${task.exception?.message}")
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
            .setMessage("Tu reserva se ha realizado con éxito. El código de tu reserva es: $reservaId")
            .setPositiveButton("Ver mis viajes") { _, _ ->
                // Ir a la actividad de Mis Viajes y pasar el ID de la nueva reserva
                val intent = Intent(this, MisViajesActivity::class.java)
                intent.putExtra("nuevaReservaId", reservaId)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .setCancelable(false)

        // Agregamos el botón para volver al destino
        if (destinoId.isNotEmpty()) {
            dialog.setNegativeButton("Volver al destino") { _, _ ->
                // Volver a la actividad DetalleVuelos con el ID del destino
                val intent = Intent(this, DetalleVuelos::class.java)
                intent.putExtra("destinoId", destinoId)
                startActivity(intent)
                finish()
            }
        } else {
            dialog.setNegativeButton("Volver al inicio") { _, _ ->
                // Si por alguna razón no tenemos el ID del destino, ir a la actividad principal
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        }

        dialog.show()
    }
}