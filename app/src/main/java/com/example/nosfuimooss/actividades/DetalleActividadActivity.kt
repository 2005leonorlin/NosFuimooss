package com.example.nosfuimooss.actividades

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Actividad
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DetalleActividadActivity : AppCompatActivity() {

    private lateinit var ivActividadImagen: ImageView
    private lateinit var btnFavorite: ImageView
    private lateinit var tvTitulo: TextView
    private lateinit var tvGrupoTipo: TextView
    private lateinit var llEstrellas: LinearLayout
    private lateinit var tvOpiniones: TextView
    private lateinit var tvPrecio: TextView
    private lateinit var tvDuracion: TextView
    private lateinit var btnReservar: Button
    private lateinit var btnSeleccionarFecha: Button

    // Contador de pasajeros
    private lateinit var btnDecrementar: ImageView
    private lateinit var btnIncrementar: ImageView
    private lateinit var tvNumPasajeros: TextView
    private lateinit var tvPrecioIndividual: TextView
    private lateinit var tvTotalLabel: TextView
    private lateinit var tvTotalPrecio: TextView

    // Secciones expandibles
    private lateinit var sectionResumen: LinearLayout
    private lateinit var sectionIncluido: LinearLayout
    private lateinit var sectionRecogida: LinearLayout
    private lateinit var sectionInformacion: LinearLayout
    private lateinit var sectionCancelacion: LinearLayout

    // Contenido expandible
    private lateinit var tvResumenContenido: TextView
    private lateinit var ivArrowResumen: ImageView
    private lateinit var ivArrowIncluido: ImageView
    private lateinit var ivArrowRecogida: ImageView
    private lateinit var ivArrowInformacion: ImageView
    private lateinit var ivArrowCancelacion: ImageView

    // Contenido expandible adicional
    private var tvIncluido: TextView? = null
    private var tvRecogida: TextView? = null
    private var tvInformacion: TextView? = null
    private var tvCancelacion: TextView? = null

    private var actividad: Actividad? = null
    private var isFavorite = false
    private val auth = FirebaseAuth.getInstance()

    // Variables para el cálculo de precio
    private var precioBase: Double = 0.0
    private var numeroPasajeros: Int = 1
    private val incrementoPorPasajero: Double = 20.0

    // Variable para controlar si se ha seleccionado fecha
    private var fechaSeleccionada: String? = null
    private var esFechaSeleccionada: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_detalle_actividad)

        // Obtener la actividad del intent
        actividad = intent.getSerializableExtra("actividad") as? Actividad

        if (actividad == null) {
            Toast.makeText(this, "Error al cargar la actividad", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupActivityData()
        createExpandableContent()
        setupClickListeners()
        checkIfFavorite()
        updatePriceDisplay()
        updateReservarButtonState() // Inicializar estado del botón
    }

    private fun initializeViews() {
        ivActividadImagen = findViewById(R.id.iv_actividad_imagen)
        btnFavorite = findViewById(R.id.btn_favorite)
        tvTitulo = findViewById(R.id.tv_titulo)
        tvGrupoTipo = findViewById(R.id.tv_grupo_tipo)
        llEstrellas = findViewById(R.id.ll_estrellas)
        tvOpiniones = findViewById(R.id.tv_opiniones)
        tvPrecio = findViewById(R.id.tv_precio)
        tvDuracion = findViewById(R.id.tv_duracion)

        btnReservar = findViewById(R.id.btn_reservar)
        btnSeleccionarFecha = findViewById(R.id.btn_seleccionar_fecha)

        // Contador de pasajeros
        btnDecrementar = findViewById(R.id.btn_decrementar)
        btnIncrementar = findViewById(R.id.btn_incrementar)
        tvNumPasajeros = findViewById(R.id.tv_num_pasajeros)
        tvPrecioIndividual = findViewById(R.id.tv_precio_individual)
        tvTotalLabel = findViewById(R.id.tv_total_label)
        tvTotalPrecio = findViewById(R.id.tv_total_precio)

        // Secciones expandibles
        sectionResumen = findViewById(R.id.section_resumen)
        sectionIncluido = findViewById(R.id.section_incluido)
        sectionRecogida = findViewById(R.id.section_recogida)
        sectionInformacion = findViewById(R.id.section_informacion)
        sectionCancelacion = findViewById(R.id.section_cancelacion)

        // Contenido y flechas
        tvResumenContenido = findViewById(R.id.tv_resumen_contenido)
        ivArrowResumen = findViewById(R.id.iv_arrow_resumen)
        ivArrowIncluido = findViewById(R.id.iv_arrow_incluido)
        ivArrowRecogida = findViewById(R.id.iv_arrow_recogida)
        ivArrowInformacion = findViewById(R.id.iv_arrow_informacion)
        ivArrowCancelacion = findViewById(R.id.iv_arrow_cancelacion)
    }

    private fun setupActivityData() {
        actividad?.let { act ->
            // Título y tipo
            tvTitulo.text = act.nombre ?: "Actividad"
            tvGrupoTipo.text = "GRUPO O PRIVADO"

            // Imagen
            if (!act.foto.isNullOrEmpty()) {
                Glide.with(this)
                    .load(act.foto)
                    .centerCrop()
                    .into(ivActividadImagen)
            }

            // Usar el precio directamente como Double
            precioBase = act.precio

            // Precio inicial - formatear el Double
            tvPrecio.text = if (precioBase > 0) {
                formatPrice(precioBase)
            } else {
                "Consultar precio"
            }

            // Duración
            tvDuracion.text = act.tiempo ?: "Duración no especificada"

            // Estrellas
            setupStars(act.estrellas)

            // Opiniones (simulado)
            val numOpiniones = (100..500).random()
            tvOpiniones.text = "$numOpiniones Opiniones"

            // Contenido de resumen - MANTENER VISIBLE por defecto
            tvResumenContenido.text = act.resumen
                ?: "Disfrute de un recorrido culturalmente enriquecedor por los lugares más famosos. En solo un día, puedes ver el resultado de cientos de años de historia y cultura local."
            tvResumenContenido.visibility = View.VISIBLE
            ivArrowResumen.rotation = 180f
        }
    }

    private fun formatPrice(price: Double): String {
        return String.format("%.2f €", price)
    }

    private fun updatePriceDisplay() {
        val precioTotal = precioBase + ((numeroPasajeros - 1) * incrementoPorPasajero)

        // Actualizar displays
        tvNumPasajeros.text = numeroPasajeros.toString()
        tvPrecioIndividual.text = formatPrice(precioBase)

        // Actualizar precio total en el bottom bar
        tvTotalLabel.text = if (numeroPasajeros == 1) {
            "Total (1 persona)"
        } else {
            "Total ($numeroPasajeros personas)"
        }
        tvTotalPrecio.text = formatPrice(precioTotal)

        // Actualizar estado de botones
        btnDecrementar.alpha = if (numeroPasajeros > 1) 1.0f else 0.5f
        btnIncrementar.alpha = if (numeroPasajeros < 10) 1.0f else 0.5f
    }

    private fun updateReservarButtonState() {
        if (esFechaSeleccionada) {
            // Botón habilitado
            btnReservar.isEnabled = true
            btnReservar.alpha = 1.0f
            btnReservar.text = "RESERVAR"
        } else {
            // Botón deshabilitado
            btnReservar.isEnabled = false
            btnReservar.alpha = 0.6f
            btnReservar.text = "SELECCIONA UNA FECHA"
        }
    }

    private fun setupStars(rating: Double) {
        llEstrellas.removeAllViews()
        val fullStars = rating.toInt()
        val hasHalfStar = rating - fullStars >= 0.5

        // Añadir estrellas completas
        for (i in 0 until fullStars) {
            val star = ImageView(this)
            star.setImageResource(R.drawable.ic_star)
            val params = LinearLayout.LayoutParams(48, 48)
            params.setMargins(4, 0, 4, 0)
            star.layoutParams = params
            star.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_orange_light))
            llEstrellas.addView(star)
        }

        // Añadir media estrella si es necesario
        if (hasHalfStar) {
            val star = ImageView(this)
            star.setImageResource(R.drawable.ic_star)
            val params = LinearLayout.LayoutParams(48, 48)
            params.setMargins(4, 0, 4, 0)
            star.layoutParams = params
            star.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_orange_light))
            llEstrellas.addView(star)
        }

        // Añadir estrellas vacías
        val totalStars = if (hasHalfStar) fullStars + 1 else fullStars
        for (i in totalStars until 5) {
            val star = ImageView(this)
            star.setImageResource(R.drawable.ic_star_outline)
            val params = LinearLayout.LayoutParams(48, 48)
            params.setMargins(4, 0, 4, 0)
            star.layoutParams = params
            star.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
            llEstrellas.addView(star)
        }
    }

    private fun createExpandableContent() {
        actividad?.let { act ->
            // Crear TextView para "Qué está incluido"
            tvIncluido = TextView(this).apply {
                text = act.queEstaIncluido
                    ?: "• Guía profesional certificado\n• Transporte incluido durante el recorrido\n• Entrada a lugares principales\n• Mapa y material informativo\n• Seguro de responsabilidad civil"
                textSize = 14f
                setTextColor(
                    ContextCompat.getColor(
                        this@DetalleActividadActivity,
                        android.R.color.darker_gray
                    )
                )
                val paddingInDp = (12 * resources.displayMetrics.density).toInt()
                setPadding(0, paddingInDp, 0, 0)
                visibility = View.GONE
                setLineSpacing(4f, 1.2f)
            }

            // Crear TextView para "Punto de recogida"
            tvRecogida = TextView(this).apply {
                text = act.puntoRecogida
                    ?: "📍 Punto de encuentro en la entrada principal del destino seleccionado\n\n⏰ Por favor, llegue 15 minutos antes de la hora programada\n\n🚌 El transporte estará claramente identificado con nuestro logo"
                textSize = 14f
                setTextColor(
                    ContextCompat.getColor(
                        this@DetalleActividadActivity,
                        android.R.color.darker_gray
                    )
                )
                val paddingInDp = (12 * resources.displayMetrics.density).toInt()
                setPadding(0, paddingInDp, 0, 0)
                visibility = View.GONE
                setLineSpacing(4f, 1.2f)
            }

            // Crear TextView para "Información"
            tvInformacion = TextView(this).apply {
                text =
                    "👥 Actividad apta para todas las edades\n\n👟 Se recomienda llevar calzado cómodo para caminar\n\n⏱️ Duración: ${act.tiempo ?: "Consultar"}\n\n🌤️ La actividad se realiza con cualquier clima\n\n📱 Traiga su cámara para capturar momentos únicos"
                textSize = 14f
                setTextColor(
                    ContextCompat.getColor(
                        this@DetalleActividadActivity,
                        android.R.color.darker_gray
                    )
                )
                val paddingInDp = (12 * resources.displayMetrics.density).toInt()
                setPadding(0, paddingInDp, 0, 0)
                visibility = View.GONE
                setLineSpacing(4f, 1.2f)
            }

            // Crear TextView para "Política de cancelación"
            tvCancelacion = TextView(this).apply {
                text =
                    "✅ Cancelación gratuita hasta 24 horas antes del inicio\n\n💰 Reembolso completo si se cancela con 24h de antelación\n\n❌ No se permite cancelación el mismo día de la actividad\n\n📞 Para cancelaciones, contacte a nuestro servicio al cliente"
                textSize = 14f
                setTextColor(
                    ContextCompat.getColor(
                        this@DetalleActividadActivity,
                        android.R.color.darker_gray
                    )
                )
                val paddingInDp = (12 * resources.displayMetrics.density).toInt()
                setPadding(0, paddingInDp, 0, 0)
                visibility = View.GONE
                setLineSpacing(4f, 1.2f)
            }

            // Añadir las vistas a sus respectivos containers
            sectionIncluido.addView(tvIncluido)
            sectionRecogida.addView(tvRecogida)
            sectionInformacion.addView(tvInformacion)
            sectionCancelacion.addView(tvCancelacion)
        }
    }

    private fun setupClickListeners() {
        // Botón favorito
        btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        // Botón para seleccionar fecha
        btnSeleccionarFecha.setOnClickListener {
            showDatePickerWithActivityDates()
        }

        // Botones de contador de pasajeros
        btnDecrementar.setOnClickListener {
            if (numeroPasajeros > 1) {
                numeroPasajeros--
                updatePriceDisplay()
            }
        }

        btnIncrementar.setOnClickListener {
            if (numeroPasajeros < 10) { // Límite máximo de 10 pasajeros
                numeroPasajeros++
                updatePriceDisplay()
            }
        }

        btnReservar.setOnClickListener {
            // Validar que se haya seleccionado una fecha
            if (!esFechaSeleccionada) {
                Toast.makeText(
                    this,
                    "Por favor, selecciona una fecha antes de reservar",
                    Toast.LENGTH_LONG
                ).show()

                // Opcional: hacer scroll hasta el selector de fecha
                val scrollView = findViewById<ScrollView>(R.id.scroll_view)
                scrollView.post {
                    scrollView.smoothScrollTo(0, btnSeleccionarFecha.top - 100)
                }
                return@setOnClickListener
            }

            // Proceder con la reserva
            val precioTotal = precioBase + ((numeroPasajeros - 1) * incrementoPorPasajero)

            Toast.makeText(
                this,
                "Reservando para $numeroPasajeros persona(s)\nFecha: $fechaSeleccionada\nTotal: ${formatPrice(precioTotal)}",
                Toast.LENGTH_LONG
            ).show()
        }

        // Secciones expandibles
        sectionResumen.setOnClickListener {
            toggleSection(tvResumenContenido, ivArrowResumen)
        }

        sectionIncluido.setOnClickListener {
            toggleSection(tvIncluido, ivArrowIncluido)
        }

        sectionRecogida.setOnClickListener {
            toggleSection(tvRecogida, ivArrowRecogida)
        }

        sectionInformacion.setOnClickListener {
            toggleSection(tvInformacion, ivArrowInformacion)
        }

        sectionCancelacion.setOnClickListener {
            toggleSection(tvCancelacion, ivArrowCancelacion)
        }
    }

    private fun showDatePickerWithActivityDates() {
        val actividad = this.actividad ?: return
        val fechaInicio = actividad.fechaInicio
        val fechaFinal = actividad.fechaFinal

        if (fechaInicio.isNullOrEmpty() || fechaFinal.isNullOrEmpty()) {
            Toast.makeText(this, "No hay fechas disponibles para esta actividad", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = dateFormat.parse(fechaInicio)
            val endDate = dateFormat.parse(fechaFinal)

            if (startDate == null || endDate == null || startDate.after(endDate)) {
                Toast.makeText(this, "Fechas de actividad no válidas", Toast.LENGTH_SHORT).show()
                return
            }

            val calendar = Calendar.getInstance()
            calendar.time = startDate
            val startYear = calendar.get(Calendar.YEAR)
            val startMonth = calendar.get(Calendar.MONTH)
            val startDay = calendar.get(Calendar.DAY_OF_MONTH)

            calendar.time = endDate
            val endYear = calendar.get(Calendar.YEAR)
            val endMonth = calendar.get(Calendar.MONTH)
            val endDay = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, dayOfMonth)
                    val displayFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
                    val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    // Actualizar el texto del botón
                    btnSeleccionarFecha.text = displayFormat.format(selectedDate.time)

                    // Guardar la fecha seleccionada
                    fechaSeleccionada = fullDateFormat.format(selectedDate.time)
                    esFechaSeleccionada = true

                    // Actualizar el estado del botón reservar
                    updateReservarButtonState()

                    Toast.makeText(
                        this,
                        "Fecha seleccionada: ${displayFormat.format(selectedDate.time)}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                startYear,
                startMonth,
                startDay
            )

            // Establecer rango de fechas permitido
            datePickerDialog.datePicker.minDate = startDate.time
            datePickerDialog.datePicker.maxDate = endDate.time

            datePickerDialog.show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar las fechas de la actividad", Toast.LENGTH_SHORT).show()
            Log.e("DatePicker", "Error al parsear fechas", e)
        }
    }

    private fun toggleSection(contentView: TextView?, arrowView: ImageView) {
        contentView?.let { content ->
            if (content.visibility == View.VISIBLE) {
                // Contraer sección
                content.visibility = View.GONE
                arrowView.animate()
                    .rotation(0f)
                    .setDuration(200)
                    .start()
            } else {
                // Expandir sección
                content.visibility = View.VISIBLE
                arrowView.animate()
                    .rotation(180f)
                    .setDuration(200)
                    .start()

                // Scroll suave para mostrar el contenido expandido
                val scrollView = findViewById<ScrollView>(R.id.scroll_view)
                scrollView.post {
                    scrollView.smoothScrollTo(0, content.bottom)
                }
            }
        }
    }

    private fun checkIfFavorite() {
        val userId = auth.currentUser?.uid ?: return
        val actividadId = actividad?.id ?: return

        val ref = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("favorites_activities")
            .child(actividadId)

        ref.get().addOnSuccessListener { snapshot ->
            isFavorite = snapshot.exists()
            updateFavoriteButton()
        }.addOnFailureListener {
            // Si hay error, asumimos que no es favorito
            isFavorite = false
            updateFavoriteButton()
        }
    }

    private fun updateFavoriteButton() {
        if (isFavorite) {
            btnFavorite.setImageResource(R.drawable.ic_corazon_completo)
            btnFavorite.setColorFilter(
                ContextCompat.getColor(
                    this,
                    android.R.color.holo_red_light
                )
            )
        } else {
            btnFavorite.setImageResource(R.drawable.ic_megusta)
            btnFavorite.setColorFilter(ContextCompat.getColor(this, android.R.color.black))
        }
    }

    private fun toggleFavorite() {
        val userId = auth.currentUser?.uid ?: return
        val actividadId = actividad?.id ?: return

        val ref = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("favorites_activities")
            .child(actividadId)

        if (isFavorite) {
            // Remover de favoritos
            ref.removeValue().addOnSuccessListener {
                isFavorite = false
                updateFavoriteButton()
                Toast.makeText(this, "Removido de favoritos", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Error al remover de favoritos", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Añadir a favoritos
            val favoriteData = mapOf(
                "id" to actividadId,
                "nombre" to (actividad?.nombre ?: ""),
                "ubicacion" to (actividad?.ubicacion ?: ""),
                "precio" to (actividad?.precio ?: 0.0), // Guardamos el precio como Double
                "foto" to (actividad?.foto ?: ""),
                "timestamp" to System.currentTimeMillis()
            )

            ref.setValue(favoriteData).addOnSuccessListener {
                isFavorite = true
                updateFavoriteButton()
                Toast.makeText(this, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Error al añadir a favoritos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}