package com.example.nosfuimooss.navegador

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.NosFuimooss.R
import com.example.nosfuimooss.boleto.VuelosActivity
import com.example.nosfuimooss.model.User
import com.example.nosfuimooss.usuario.Reserva
import com.example.nosfuimooss.usuariologeado.UsuarioLogeadoInicial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Calendario : AppCompatActivity() {
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var monthYearText: TextView
    private lateinit var selectedDateText: TextView
    private lateinit var eventsText: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton

    private lateinit var calendarAdapter: CalendarAdapter
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
    private val dayFormatter = SimpleDateFormat("d 'de' MMMM, yyyy", Locale("es", "ES"))

    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private lateinit var database: DatabaseReference

    // Lista de reservas
    private var reservas: MutableList<Reserva> = mutableListOf()
    private var selectedDate: Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendario)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        initFirebase()
        loadUserData()
        setupCalendar()
        loadReservas()
        setupClickListeners()
    }

    private fun initViews() {
        calendarRecyclerView = findViewById(R.id.calendar_recycler_view)
        monthYearText = findViewById(R.id.tv_month_year)
        selectedDateText = findViewById(R.id.selected_date_text)
        eventsText = findViewById(R.id.events_text)
        btnPrevMonth = findViewById(R.id.btn_prev_month)
        btnNextMonth = findViewById(R.id.btn_next_month)
    }

    private fun initFirebase() {
        database = FirebaseDatabase.getInstance().reference
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { selectedDay ->
            onDateSelected(selectedDay)
        }

        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarRecyclerView.adapter = calendarAdapter

        updateCalendar()
    }

    private fun setupClickListeners() {
        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        // Navegación
        findViewById<View>(R.id.nav_home_container)?.setOnClickListener {
            navigateToMain()
        }

        findViewById<View>(R.id.nav_flight_container)?.setOnClickListener {
            navigateToFlights()
        }

        findViewById<View>(R.id.nav_moon_container)?.setOnClickListener {
            navigateToNights()
        }

        findViewById<View>(R.id.nav_heart_container)?.setOnClickListener {
            navigateToFavorites()
        }

        findViewById<View>(R.id.nav_profile_container)?.setOnClickListener {
            navigateToProfile()
        }
    }

    private fun updateCalendar() {
        monthYearText.text = dateFormatter.format(calendar.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        val daysInMonth = getDaysInMonth()
        calendarAdapter.updateDays(daysInMonth, getReservationDates())
    }

    private fun getDaysInMonth(): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()

        // Clonar el calendar para no modificar el original
        val tempCalendar = calendar.clone() as Calendar
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        // Días del mes anterior para completar la primera semana
        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1
        tempCalendar.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek)

        // Generar 42 días (6 semanas x 7 días)
        for (i in 0 until 42) {
            val isCurrentMonth = tempCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
            val isToday = isSameDay(tempCalendar, Calendar.getInstance())

            days.add(CalendarDay(
                day = tempCalendar.get(Calendar.DAY_OF_MONTH),
                date = tempCalendar.clone() as Calendar,
                isCurrentMonth = isCurrentMonth,
                isToday = isToday
            ))

            tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return days
    }

    private fun getReservationDates(): Set<String> {
        val reservationDates = mutableSetOf<String>()

        Log.d("Calendario", "=== PROCESANDO FECHAS DE RESERVAS (MEJORADO) ===")
        Log.d("Calendario", "Total de reservas: ${reservas.size}")

        reservas.forEach { reserva ->
            Log.d("Calendario", "Procesando reserva ID: '${reserva.id}'")

            // Debug completo de la reserva
            Log.d("Calendario", "Reserva completa:")
            Log.d("Calendario", "  - userId: '${reserva.userId}'")
            Log.d("Calendario", "  - fechaIda: ${reserva.fechaIda}")
            Log.d("Calendario", "  - fechaVuelta: ${reserva.fechaVuelta}")
            Log.d("Calendario", "  - fechaReserva: ${reserva.fechaReserva}")
            Log.d("Calendario", "  - idaDepartureTime: '${reserva.idaDepartureTime}'")
            Log.d("Calendario", "  - vueltaDepartureTime: '${reserva.vueltaDepartureTime}'")
            Log.d("Calendario", "  - roundTrip: ${reserva.roundTrip}")
            Log.d("Calendario", "  - boletoIda: ${reserva.boletoIda}")
            Log.d("Calendario", "  - boletoVuelta: ${reserva.boletoVuelta}")

            // Intentar diferentes fuentes para las fechas
            val fechasAlternativas = mutableListOf<Pair<String, Long>>()

            // 1. Usar fechaIda y fechaVuelta directamente
            if (reserva.roundTrip && reserva.fechaIda > 0 && reserva.fechaVuelta > reserva.fechaIda) {
                val idaCal = Calendar.getInstance().apply { timeInMillis = reserva.fechaIda }
                val vueltaCal = Calendar.getInstance().apply { timeInMillis = reserva.fechaVuelta }

                while (!idaCal.after(vueltaCal)) {
                    val dateKey = "${idaCal.get(Calendar.YEAR)}-${idaCal.get(Calendar.MONTH)}-${idaCal.get(Calendar.DAY_OF_MONTH)}"
                    reservationDates.add(dateKey)

                    Log.d("Calendario", "✅ Fecha del rango ida-vuelta agregada: $dateKey")

                    idaCal.add(Calendar.DAY_OF_MONTH, 1)
                }
            } else {
                // Si no es roundTrip o falta alguna fecha, añadir fechas individuales
                if (reserva.fechaIda > 0) {
                    fechasAlternativas.add("fechaIda directa" to reserva.fechaIda)
                }
                if (reserva.fechaVuelta > 0) {
                    fechasAlternativas.add("fechaVuelta directa" to reserva.fechaVuelta)
                }
            }


            // 2. Usar fechaReserva si las otras están vacías
            if (reserva.fechaReserva > 0) {
                fechasAlternativas.add("fechaReserva" to reserva.fechaReserva)
            }

            // 3. Intentar parsear las fechas de los strings de departure time
            try {
                if (reserva.idaDepartureTime.isNotEmpty()) {
                    val parsedDate = parseDateFromString(reserva.idaDepartureTime)
                    if (parsedDate > 0) {
                        fechasAlternativas.add("idaDepartureTime parseado" to parsedDate)
                    }
                }
                if (reserva.vueltaDepartureTime.isNotEmpty()) {
                    val parsedDate = parseDateFromString(reserva.vueltaDepartureTime)
                    if (parsedDate > 0) {
                        fechasAlternativas.add("vueltaDepartureTime parseado" to parsedDate)
                    }
                }
            } catch (e: Exception) {
                Log.w("Calendario", "Error parseando departure times: ${e.message}")
            }

            // 4. Intentar usar fechas de los boletos si existen
            reserva.boletoIda?.let { boleto ->
                if (boleto.fechaInicial.isNotEmpty()) {
                    try {
                        val parsedDate = parseDateFromString(boleto.fechaInicial)
                        if (parsedDate > 0) {
                            fechasAlternativas.add("boletoIda.fechaInicial" to parsedDate)
                        }
                    } catch (e: Exception) {
                        Log.w("Calendario", "Error parseando fecha inicial del boleto ida: ${e.message}")
                    }
                }
            }

            reserva.boletoVuelta?.let { boleto ->
                if (boleto.fechaInicial.isNotEmpty()) {
                    try {
                        val parsedDate = parseDateFromString(boleto.fechaInicial)
                        if (parsedDate > 0) {
                            fechasAlternativas.add("boletoVuelta.fechaInicial" to parsedDate)
                        }
                    } catch (e: Exception) {
                        Log.w("Calendario", "Error parseando fecha inicial del boleto vuelta: ${e.message}")
                    }
                }
            }

            // Procesar todas las fechas encontradas
            Log.d("Calendario", "Fechas alternativas encontradas: ${fechasAlternativas.size}")
            fechasAlternativas.forEach { (fuente, timestamp) ->
                try {
                    val fechaCalendar = Calendar.getInstance().apply {
                        timeInMillis = timestamp
                    }

                    // Normalizar a medianoche
                    fechaCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    fechaCalendar.set(Calendar.MINUTE, 0)
                    fechaCalendar.set(Calendar.SECOND, 0)
                    fechaCalendar.set(Calendar.MILLISECOND, 0)

                    val dateKey = "${fechaCalendar.get(Calendar.YEAR)}-${fechaCalendar.get(Calendar.MONTH)}-${fechaCalendar.get(Calendar.DAY_OF_MONTH)}"
                    reservationDates.add(dateKey)

                    Log.d("Calendario", "✅ Fecha agregada desde $fuente:")
                    Log.d("Calendario", "   Timestamp: $timestamp")
                    Log.d("Calendario", "   Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))}")
                    Log.d("Calendario", "   Key: $dateKey")

                } catch (e: Exception) {
                    Log.e("Calendario", "❌ Error procesando fecha de $fuente: ${e.message}")
                }
            }

            if (fechasAlternativas.isEmpty()) {
                Log.w("Calendario", "⚠️ No se encontraron fechas válidas para la reserva ${reserva.id}")
            }
        }

        Log.d("Calendario", "=== RESULTADO FINAL MEJORADO ===")
        Log.d("Calendario", "Total fechas con reservas: ${reservationDates.size}")
        Log.d("Calendario", "Fechas: ${reservationDates.joinToString(", ")}")

        return reservationDates
    }

    // Función para intentar parsear fechas de diferentes formatos
    private fun parseDateFromString(dateString: String): Long {
        val formatters = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        )

        for (formatter in formatters) {
            try {
                val date = formatter.parse(dateString)
                if (date != null) {
                    Log.d("Calendario", "✅ Fecha parseada exitosamente: $dateString -> ${date.time}")
                    return date.time
                }
            } catch (e: Exception) {
                // Continuar con el siguiente formato
            }
        }

        Log.w("Calendario", "❌ No se pudo parsear la fecha: $dateString")
        return 0L
    }

    private fun onDateSelected(calendarDay: CalendarDay) {
        selectedDate = calendarDay.date
        selectedDateText.text = "Fecha seleccionada: ${dayFormatter.format(calendarDay.date.time)}"

        // Buscar eventos en esta fecha
        val eventsForDate = getEventsForDate(calendarDay.date)
        if (eventsForDate.isNotEmpty()) {
            eventsText.text = eventsForDate.joinToString("\n")
        } else {
            eventsText.text = "No hay eventos programados para este día"
        }

        calendarAdapter.setSelectedDate(calendarDay.date)
    }

    private fun getEventsForDate(date: Calendar): List<String> {
        val events = mutableListOf<String>()

        // Normalizar la fecha seleccionada
        val selectedCal = date.clone() as Calendar
        selectedCal.set(Calendar.HOUR_OF_DAY, 0)
        selectedCal.set(Calendar.MINUTE, 0)
        selectedCal.set(Calendar.SECOND, 0)
        selectedCal.set(Calendar.MILLISECOND, 0)

        reservas.forEach { reserva ->
            // Obtener todas las fechas posibles para esta reserva
            val fechasPosibles = mutableListOf<Pair<String, Long>>()

            if (reserva.fechaIda > 0) fechasPosibles.add("ida" to reserva.fechaIda)
            if (reserva.fechaVuelta > 0) fechasPosibles.add("vuelta" to reserva.fechaVuelta)
            if (reserva.fechaReserva > 0) fechasPosibles.add("reserva" to reserva.fechaReserva)

            // Intentar parsear departure times
            try {
                if (reserva.idaDepartureTime.isNotEmpty()) {
                    val parsed = parseDateFromString(reserva.idaDepartureTime)
                    if (parsed > 0) fechasPosibles.add("ida_departure" to parsed)
                }
                if (reserva.vueltaDepartureTime.isNotEmpty()) {
                    val parsed = parseDateFromString(reserva.vueltaDepartureTime)
                    if (parsed > 0) fechasPosibles.add("vuelta_departure" to parsed)
                }
            } catch (e: Exception) {
                Log.w("Calendario", "Error parseando departure times para eventos: ${e.message}")
            }

            fechasPosibles.forEach { (tipo, timestamp) ->
                val fechaCal = Calendar.getInstance().apply {
                    timeInMillis = timestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (isSameDay(selectedCal, fechaCal)) {
                    when (tipo) {
                        "ida", "ida_departure" -> {
                            val origen = reserva.boletoIda?.origen ?: "Origen desconocido"
                            val destino = reserva.boletoIda?.destino ?: "Destino desconocido"
                            events.add("✈️ Vuelo de ida - $origen → $destino")
                        }
                        "vuelta", "vuelta_departure" -> {
                            val origen = reserva.boletoVuelta?.origen ?: reserva.boletoIda?.destino ?: "Origen desconocido"
                            val destino = reserva.boletoVuelta?.destino ?: reserva.boletoIda?.origen ?: "Destino desconocido"
                            events.add("🔄 Vuelo de regreso - $origen → $destino")
                        }
                        "reserva" -> {
                            events.add("📅 Reserva realizada")
                        }
                    }
                }
            }
        }

        return events.distinct() // Eliminar duplicados
    }
    private fun loadUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userNameText = findViewById<TextView>(R.id.user_name_text)

        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                userNameText.text = if (user?.name?.isNotBlank() == true)
                    "${user.name}"
                else
                    "Hola, usuario"
            }
            .addOnFailureListener {
                userNameText.text = "Hola, usuario"
            }
    }

    private fun loadReservas() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("Calendario", "Usuario no autenticado")
            return
        }


        Log.d("Calendario", "=== CARGANDO RESERVAS (MEJORADO) ===")
        Log.d("Calendario", "Usuario ID: $userId")

        database.child("reservas").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    reservas.clear()
                    Log.d("Calendario", "Total reservas en BD: ${snapshot.childrenCount}")

                    for (reservaSnap in snapshot.children) {
                        Log.d("Calendario", "Intentando mapear reserva: ${reservaSnap.key}")
                        val reserva = reservaSnap.getValue(Reserva::class.java)
                        if (reserva == null) {
                            Log.e("Calendario", "❌ No se pudo mapear la reserva ${reservaSnap.key}")
                            continue
                        }

                        // IMPORTANTE: establece el ID de la reserva desde la key
                        reserva.id = reservaSnap.key ?: ""

                        reservas.add(reserva)
                        Log.d("Calendario", "  → Reserva cargada: ID=${reserva.id}, fechaIda=${reserva.fechaIda}")
                    }

                    Log.d("Calendario", "=== RESUMEN CARGA MEJORADO === Total reservas cargadas: ${reservas.size}")
                    updateCalendar()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Calendario", "Error al cargar reservas: ${error.message}")
                }
            })
    }


    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    // Métodos de navegación
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
}

// Data class para representar un día del calendario
data class CalendarDay(
    val day: Int,
    val date: Calendar,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

// Adapter para el RecyclerView del calendario
class CalendarAdapter(
    private val onDateClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var days: List<CalendarDay> = emptyList()
    private var reservationDates: Set<String> = emptySet()
    private var selectedDate: Calendar? = null

    fun updateDays(newDays: List<CalendarDay>, newReservationDates: Set<String>) {
        days = newDays
        reservationDates = newReservationDates
        Log.d("CalendarAdapter", "Actualizando días. Total días: ${newDays.size}, Fechas con reservas: ${newReservationDates.size}")
        Log.d("CalendarAdapter", "Fechas con reservas: ${newReservationDates.joinToString(", ")}")
        notifyDataSetChanged()
    }

    fun setSelectedDate(date: Calendar) {
        selectedDate = date
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(days[position])
    }



    override fun getItemCount(): Int = days.size

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayText: TextView = itemView.findViewById(R.id.day_text)
        private val dayContainer: View = itemView.findViewById(R.id.day_container)

        fun bind(calendarDay: CalendarDay) {
            dayText.text = calendarDay.day.toString()

            // Configurar colores y estados
            when {
                !calendarDay.isCurrentMonth -> {
                    dayText.setTextColor(Color.parseColor("#CCCCCC"))
                    dayContainer.background = null
                }
                calendarDay.isToday -> {
                    dayText.setTextColor(Color.WHITE)
                    dayContainer.setBackgroundResource(R.drawable.calendar_today_background)
                }
                isSelected(calendarDay) -> {
                    dayText.setTextColor(Color.WHITE)
                    dayContainer.setBackgroundResource(R.drawable.calendar_selected_background)
                }
                hasReservation(calendarDay) -> {
                    dayText.setTextColor(Color.WHITE)
                    dayContainer.setBackgroundResource(R.drawable.calendar_reservation_background)
                    Log.d("CalendarAdapter", "✅ Día ${calendarDay.day} tiene reserva - Mes actual: ${calendarDay.isCurrentMonth}")
                }
                else -> {
                    dayText.setTextColor(Color.BLACK)
                    dayContainer.background = null
                }
            }

            itemView.setOnClickListener {
                if (calendarDay.isCurrentMonth) {
                    onDateClick(calendarDay)
                }
            }
        }

        private fun isSelected(calendarDay: CalendarDay): Boolean {
            return selectedDate?.let { selected ->
                selected.get(Calendar.YEAR) == calendarDay.date.get(Calendar.YEAR) &&
                        selected.get(Calendar.MONTH) == calendarDay.date.get(Calendar.MONTH) &&
                        selected.get(Calendar.DAY_OF_MONTH) == calendarDay.date.get(Calendar.DAY_OF_MONTH)
            } ?: false
        }

        private fun hasReservation(calendarDay: CalendarDay): Boolean {
            val dateKey = "${calendarDay.date.get(Calendar.YEAR)}-${calendarDay.date.get(Calendar.MONTH)}-${calendarDay.date.get(Calendar.DAY_OF_MONTH)}"
            val hasReservation = reservationDates.contains(dateKey)

            if (hasReservation) {
                Log.d("CalendarAdapter", "✅ Día ${calendarDay.day} coincide con reserva. Key: $dateKey")
            }

            return hasReservation
        }
    }
}