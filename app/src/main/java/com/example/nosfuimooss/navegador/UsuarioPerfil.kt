package com.example.nosfuimooss.navegador

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.User
import com.example.nosfuimooss.nologeado.UsuarioNoLogeadoInicioActivity
import com.example.nosfuimooss.usuariologeado.UsuarioLogeadoInicial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UsuarioPerfil : AppCompatActivity() {
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var userApellidoText: TextView
    private lateinit var userFullNameText: TextView
    private lateinit var userEmailDetailedText: TextView

    // Firebase references
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference

    // SharedPreferences para mantener datos localmente
    private lateinit var sharedPreferences: SharedPreferences

    // Usuario actual
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        enableEdgeToEdge()
        setContentView(R.layout.activity_usuario_perfil)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initFirebase()
        setupUI()
        loadUserData()
        setupClickListeners()
    }

    private fun initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
    }

    private fun setupUI() {
        // Referencias a TODOS los elementos de la UI relacionados con datos del usuario
        userNameText = findViewById(R.id.user_name_text)
        userEmailText = findViewById(R.id.user_email_text)
        userApellidoText = findViewById(R.id.user_apellido_text)
        userFullNameText = findViewById(R.id.user_full_name)
        userEmailDetailedText = findViewById(R.id.user_email_detailed)

        // Mostrar datos guardados localmente mientras se cargan los de Firebase
        loadLocalUserData()
    }

    private fun loadLocalUserData() {
        val savedName = sharedPreferences.getString("user_name", "")
        val savedApellido = sharedPreferences.getString("user_apellido", "")
        val savedEmail = sharedPreferences.getString("user_email", "")

        if (!savedName.isNullOrEmpty()) {
            userNameText.text = savedName
            userApellidoText.text = savedApellido
            userEmailText.text = savedEmail
            userEmailDetailedText.text = savedEmail
            userFullNameText.text = "$savedName $savedApellido"
        }
    }

    private fun loadUserData() {
        val currentFirebaseUser = firebaseAuth.currentUser

        if (currentFirebaseUser != null) {
            val userId = currentFirebaseUser.uid
            val userEmail = currentFirebaseUser.email ?: ""

            Log.d("UsuarioPerfil", "Usuario autenticado: $userId, Email: $userEmail")

            // Cargar datos desde Firebase Realtime Database
            databaseRef.child("users").child(userId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            try {
                                // Obtener datos del usuario desde Firebase
                                val firebaseUser = dataSnapshot.getValue(User::class.java)

                                if (firebaseUser != null) {
                                    // Asegurar que el email esté presente (usar el de Auth si no está en Database)
                                    val completeUser = firebaseUser.copy(
                                        email = if (firebaseUser.email.isNotEmpty()) firebaseUser.email else userEmail
                                    )

                                    currentUser = completeUser
                                    updateUI(completeUser)
                                    saveUserDataLocally(completeUser)

                                    Log.d("UsuarioPerfil", "Datos cargados desde Firebase: $completeUser")
                                } else {
                                    // Si no hay datos en Firebase, crear usuario con email de Auth
                                    createDefaultUser(userEmail, userId)
                                }
                            } catch (e: Exception) {
                                Log.e("UsuarioPerfil", "Error al parsear usuario: ${e.message}")
                                createDefaultUser(userEmail, userId)
                            }
                        } else {
                            // Usuario no existe en la base de datos, crear con datos básicos
                            Log.d("UsuarioPerfil", "Usuario no existe en DB, creando con email: $userEmail")
                            createDefaultUser(userEmail, userId)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("UsuarioPerfil", "Error al cargar usuario: ${databaseError.message}")
                        showErrorMessage("Error al conectar con la base de datos")

                        // En caso de error, al menos mostrar el email del usuario autenticado
                        val fallbackUser = User(
                            name = "Usuario",
                            apellido = "",
                            email = userEmail
                        )
                        updateUI(fallbackUser)
                    }
                })
        } else {
            // Usuario no autenticado, redirigir al login
            Log.w("UsuarioPerfil", "Usuario no autenticado")
            redirectToLogin()
        }
    }

    private fun createDefaultUser(email: String, userId: String) {
        val defaultUser = User(
            name = "Usuario",
            apellido = "",
            email = email
        )

        // Guardar usuario básico en Firebase
        saveUserToFirebase(userId, defaultUser)
        currentUser = defaultUser
        updateUI(defaultUser)
        saveUserDataLocally(defaultUser)

        Log.d("UsuarioPerfil", "Usuario por defecto creado con email: $email")
    }

    private fun saveUserToFirebase(userId: String, user: User) {
        databaseRef.child("users").child(userId).setValue(user)
            .addOnSuccessListener {
                Log.d("UsuarioPerfil", "Usuario guardado en Firebase correctamente")
            }
            .addOnFailureListener { e ->
                Log.e("UsuarioPerfil", "Error al guardar usuario: ${e.message}")
            }
    }

    private fun updateUI(user: User) {
        runOnUiThread {
            val displayName = if (user.name.isNotEmpty()) user.name else "Usuario"
            val displayApellido = if (user.apellido.isNotEmpty()) user.apellido else ""
            val displayEmail = if (user.email.isNotEmpty()) user.email else "Sin email"

            // Actualizar todos los campos de la UI
            userNameText.text = displayName
            userApellidoText.text = if (displayApellido.isNotEmpty()) displayApellido else "Sin apellido"
            userEmailText.text = displayEmail
            userEmailDetailedText.text = displayEmail

            // Actualizar el nombre completo
            val fullName = if (displayApellido.isNotEmpty()) {
                "$displayName $displayApellido"
            } else {
                displayName
            }
            userFullNameText.text = fullName

            Log.d("UsuarioPerfil", "UI actualizada - Nombre: $displayName, Email: $displayEmail")
        }
    }

    private fun saveUserDataLocally(user: User) {
        sharedPreferences.edit().apply {
            putString("user_name", user.name)
            putString("user_apellido", user.apellido)
            putString("user_email", user.email)
            apply()
        }
        Log.d("UsuarioPerfil", "Datos guardados localmente")
    }

    private fun showErrorMessage(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun redirectToLogin() {
        Toast.makeText(this, "Sesión expirada. Por favor, inicia sesión nuevamente.", Toast.LENGTH_LONG).show()
        // Redirigir a la pantalla de login
        val intent = Intent(this, UsuarioNoLogeadoInicioActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupClickListeners() {
        // Cerrar sesión
        findViewById<LinearLayout>(R.id.logout_option).setOnClickListener {
            showLogoutDialog()
        }

        // Condiciones de uso
        findViewById<LinearLayout>(R.id.terms_option).setOnClickListener {
            showTermsDialog()
        }

        // Política de privacidad
        findViewById<LinearLayout>(R.id.privacy_option).setOnClickListener {
            showPrivacyDialog()
        }

        // Calcular precio
        findViewById<LinearLayout>(R.id.calculate_price_option).setOnClickListener {
            showCalculatePriceDialog()
        }

        // Bottom navigation
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        findViewById<ImageView>(R.id.nav_home).setOnClickListener {
            Toast.makeText(this, "Destinos", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, UsuarioLogeadoInicial::class.java))
        }
        findViewById<ImageView>(R.id.nav_flight).setOnClickListener {
            Toast.makeText(this, "Búsqueda de vuelos", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ElegirVuelo::class.java))
        }
        findViewById<ImageView>(R.id.nav_moon).setOnClickListener {
            Toast.makeText(this, "Mis Viajes", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MisViajesActivity::class.java))
        }
        findViewById<ImageView>(R.id.nav_heart).setOnClickListener {
            Toast.makeText(this, "Favoritos", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Favoritos::class.java))
        }

        findViewById<ImageView>(R.id.ic_calendar).setOnClickListener {
            startActivity(Intent(this, Calendario::class.java))
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        // Cerrar sesión en Firebase
        firebaseAuth.signOut()

        // Limpiar datos locales
        sharedPreferences.edit().clear().apply()

        // Navegar a UsuarioNoLogeado
        val intent = Intent(this, UsuarioNoLogeadoInicioActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showTermsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Condiciones de Uso")
            .setMessage("""
                CONDICIONES DE USO - NosFuimooss
                
                1. ACEPTACIÓN DE TÉRMINOS
                Al utilizar esta aplicación, aceptas cumplir con estos términos y condiciones.
                
                2. USO DE LA APLICACIÓN
                Esta aplicación está destinada para la planificación y reserva de viajes. El usuario se compromete a usar la aplicación de manera responsable.
                
                3. RESERVAS Y PAGOS
                Todas las reservas están sujetas a disponibilidad. Los precios pueden variar según la temporada y disponibilidad.
                
                4. CANCELACIONES
                Las políticas de cancelación varían según el proveedor de servicios. Consulta los términos específicos antes de realizar una reserva.
                
                5. LIMITACIÓN DE RESPONSABILIDAD
                NosFuimooss actúa como intermediario entre usuarios y proveedores de servicios de viaje.
                
                Para más información, contacta nuestro soporte.
            """.trimIndent())
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun showPrivacyDialog() {
        AlertDialog.Builder(this)
            .setTitle("Política de Privacidad")
            .setMessage("""
                POLÍTICA DE PRIVACIDAD - NosFuimooss
                
                1. INFORMACIÓN QUE RECOPILAMOS
                Recopilamos información personal como nombre, email, y preferencias de viaje para mejorar tu experiencia.
                
                2. USO DE LA INFORMACIÓN
                Utilizamos tu información para:
                - Procesar reservas
                - Personalizar recomendaciones
                - Enviar confirmaciones y actualizaciones
                - Mejorar nuestros servicios
                
                3. PROTECCIÓN DE DATOS
                Implementamos medidas de seguridad para proteger tu información personal.
                
                4. COMPARTIR INFORMACIÓN
                No vendemos ni compartimos tu información personal con terceros, excepto cuando sea necesario para procesar tu reserva.
                
                5. TUS DERECHOS
                Tienes derecho a acceder, corregir o eliminar tu información personal.
                
                6. COOKIES
                Utilizamos cookies para mejorar la funcionalidad de la aplicación.
                
                Última actualización: Mayo 2025
            """.trimIndent())
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun showCalculatePriceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Calculadora de Precios")
            .setMessage("""
                CÓMO CALCULAMOS LOS PRECIOS
                
                Nuestros precios se basan en varios factores:
                
                🏨 ALOJAMIENTO:
                • Temporada (alta/baja)
                • Tipo de habitación
                • Ubicación del hotel
                • Servicios incluidos
                
                ✈️ VUELOS:
                • Fechas de viaje
                • Anticipación de la reserva
                • Clase de vuelo
                • Aerolínea
                
                🚗 TRANSPORTE:
                • Distancia del recorrido
                • Tipo de vehículo
                • Combustible
                • Peajes
                
                📍 DESTINO:
                • Popularidad del lugar
                • Eventos especiales
                • Clima/temporada
                
                💡 CONSEJOS PARA AHORRAR:
                • Reserva con anticipación
                • Viaja en temporada baja
                • Compara diferentes opciones
                • Suscríbete a nuestras ofertas
                
                ¿Necesitas una cotización personalizada? Contáctanos.
            """.trimIndent())
            .setPositiveButton("Entendido", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar listeners de Firebase si es necesario
    }
}