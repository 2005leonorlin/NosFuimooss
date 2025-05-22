package com.example.nosfuimooss.boleto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Boleto
import com.example.nosfuimooss.usuario.PreferenciasUsuario
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SeleccionarBoletoIdaActivity : AppCompatActivity() {
    private lateinit var tvRoute: TextView
    private lateinit var tvPassengerInfo: TextView
    private lateinit var dateScroll: HorizontalScrollView
    private lateinit var datesContainer: LinearLayout
    private lateinit var preferenciasUsuario: PreferenciasUsuario
    private var imageUrl: String = ""
    private lateinit var boleto: Boleto
    private var isRoundTrip: Boolean = false
    private var passengerCount: Int = 1
    private var selectedDate: Date? = null
    private var basePrice: Double = 0.0
    private var selectedTime: String? = null
    private var categoria: String = "Turista"
    private var fechaInicio: Date? = null
    private var fechaFin: Date? = null

    private val timePriceList = listOf(
        "06:30" to 0,
        "11:45" to 10,
        "13:30" to 15,
        "16:30" to 25,
        "18:50" to 20,
        "20:30" to 30
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccionar_boleto_ida)
        supportActionBar?.hide()
        imageUrl = intent.getStringExtra("imageUrl") ?: ""
        tvRoute = findViewById(R.id.tvRoute)
        tvPassengerInfo = findViewById(R.id.tvPassengerInfo)
        dateScroll = findViewById(R.id.dateScroll)
        datesContainer = dateScroll.getChildAt(0) as LinearLayout

        boleto = intent.getSerializableExtra("boleto") as? Boleto ?: return finish()
        isRoundTrip = intent.getBooleanExtra("isRoundTrip", false)
        passengerCount = intent.getIntExtra("cantidadPersonas", 1)
        categoria = intent.getStringExtra("categoria") ?: "Turista"

        val preferencias = intent.getSerializableExtra("preferencias") as? PreferenciasUsuario

        if (preferencias != null) {
            preferenciasUsuario = preferencias
            fechaInicio = Date(preferencias.fechaInicio)
            fechaFin = Date(preferencias.fechaFin)
            passengerCount = preferencias.cantidadPersonas
            categoria = preferencias.categoria
        }

        val precioBaseRecibido = boleto.precio
        basePrice = (precioBaseRecibido + when (categoria) {
            "Turista Premium" -> 10
            "Business" -> 20
            "Primera Clase" -> 50
            else -> 1
        } + if (isRoundTrip) 50 else 0) * passengerCount

        tvRoute.text = "${boleto.origen} - ${boleto.destino}"
        val tripType = if (isRoundTrip) "Ida y vuelta" else "Solo ida"
        val passengers = if (passengerCount == 1) "1 adulto" else "$passengerCount adultos"
        tvPassengerInfo.text = "$tripType · $passengers · $categoria"

        setupDateScroller()
    }

    private fun formatDateForLog(date: Date?): String {
        return date?.let {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(it)
        } ?: "null"
    }

    private fun sameDay(d1: Date, d2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = d1 }
        val cal2 = Calendar.getInstance().apply { time = d2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun setupDateScroller() {
        datesContainer.removeAllViews()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = sdf.parse(boleto.fechaInicial) ?: return
        val endDate = sdf.parse(boleto.fechaFinal) ?: return

        // Si tenemos fechaInicio de las preferencias, usarla; de lo contrario, usar startDate
        selectedDate = fechaInicio

        // Log para depuración
        Log.d("SeleccionarBoletoIda", "selectedDate: ${formatDateForLog(selectedDate)}")
        Log.d("SeleccionarBoletoIda", "startDate: ${formatDateForLog(startDate)}")
        Log.d("SeleccionarBoletoIda", "endDate: ${formatDateForLog(endDate)}")

        val diffInMillis = endDate.time - startDate.time
        val daysDiff = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS).toInt() + 1

        val calendar = Calendar.getInstance().apply { time = startDate }
        val displayFormat = SimpleDateFormat("EEE dd MMM", Locale("es", "ES"))
        val monthFormat = SimpleDateFormat("MMM", Locale("es", "ES"))

        val monthView = createDateView(monthFormat.format(calendar.time).uppercase(), false)
        val monthLp = LinearLayout.LayoutParams(
            (resources.displayMetrics.density * 100).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        monthView.layoutParams = monthLp
        datesContainer.addView(monthView)

        // Variable para rastrear si encontramos y seleccionamos la fecha de inicio
        var foundSelectedDate = false
        var selectedIndex = -1

        // Primero, crear todas las vistas de fecha sin selección
        val dateViews = ArrayList<LinearLayout>()

        for (i in 0 until daysDiff) {
            val date = calendar.time
            // Verificar si esta fecha coincide con la fecha seleccionada (fechaInicio)
            val isThisDateSelected = selectedDate?.let { sameDay(date, it) } ?: false

            if (isThisDateSelected) {
                foundSelectedDate = true
                selectedIndex = datesContainer.childCount
                Log.d("SeleccionarBoletoIda",
                    "Encontrada fecha correspondiente a fechaInicio: ${formatDateForLog(selectedDate)}")
            }

            val dateView = createDateView(displayFormat.format(date), false)

            val dateLp = LinearLayout.LayoutParams(
                (resources.displayMetrics.density * 100).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            dateView.layoutParams = dateLp

            val finalDate = if (isThisDateSelected && selectedDate != null) {
                selectedDate!!
            } else {
                Date(date.time)
            }
            // Crear una copia final de la fecha
            dateView.setOnClickListener {
                // Limpiar selección anterior
                clearDateSelection()

                // Aplicar selección a este elemento
                applyDateSelection(dateView)

                selectedDate = finalDate
                setupFlightOptions()
            }

            dateViews.add(dateView)
            datesContainer.addView(dateView)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Después de crear todas las vistas, seleccionar la correcta
        if (foundSelectedDate && selectedIndex >= 0) {
            Log.d("SeleccionarBoletoIda", "Aplicando selección a la vista de fecha en posición: $selectedIndex")
            val dateViewToSelect = datesContainer.getChildAt(selectedIndex) as LinearLayout
            applyDateSelection(dateViewToSelect)
            dateScroll.post {
                dateScroll.smoothScrollTo(dateViewToSelect.left, 0)
            }
        } else if (datesContainer.childCount > 1) {
            Log.d("SeleccionarBoletoIda", "No se encontró fechaInicio en el rango, seleccionando primera fecha")
            selectedDate = startDate
            val firstDateView = datesContainer.getChildAt(1) as LinearLayout
            applyDateSelection(firstDateView)
        }

        setupFlightOptions()
    }

    private fun clearDateSelection() {
        // Limpiar selección anterior
        for (j in 1 until datesContainer.childCount) {
            val view = datesContainer.getChildAt(j)
            view.setBackgroundColor(resources.getColor(android.R.color.transparent))
            val tv = (view as LinearLayout).getChildAt(0) as TextView
            tv.setTextColor(resources.getColor(android.R.color.darker_gray))
            tv.textSize = 16f
            if (view.childCount > 1) view.removeViewAt(1)
        }
    }

    private fun applyDateSelection(dateView: LinearLayout) {
        // Aplicar selección a este elemento
        dateView.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
        val tvSel = dateView.getChildAt(0) as TextView
        tvSel.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        tvSel.textSize = 16f
        val indicator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (resources.displayMetrics.density * 60).toInt(),
                (resources.displayMetrics.density * 3).toInt()
            ).also { it.topMargin = (resources.displayMetrics.density * 8).toInt() }
            setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
        }
        dateView.addView(indicator)
    }

    private fun createDateView(dateText: String, isSelected: Boolean): LinearLayout {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(16, 16, 16, 16)

        val textView = TextView(this)
        textView.text = dateText
        textView.textSize = 16f
        textView.setTextColor(
            if (isSelected) resources.getColor(android.R.color.holo_green_dark)
            else resources.getColor(android.R.color.darker_gray)
        )
        layout.addView(textView)

        if (isSelected) {
            layout.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
            val indicator = View(this)
            indicator.layoutParams = LinearLayout.LayoutParams(
                (resources.displayMetrics.density * 60).toInt(),
                (resources.displayMetrics.density * 3).toInt()
            ).also { it.topMargin = (resources.displayMetrics.density * 8).toInt() }
            indicator.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            layout.addView(indicator)
        }

        return layout
    }

    private fun setupFlightOptions() {
        val flightContainer = findViewById<LinearLayout>(R.id.flightOptionsContainer)
        flightContainer.removeAllViews()

        val shuffled = timePriceList.shuffled().take(3)
        for ((time, priceAdd) in shuffled) {
            val card = createFlightCard(time, priceAdd)
            flightContainer.addView(card)
            (card.layoutParams as LinearLayout.LayoutParams).bottomMargin = (resources.displayMetrics.density * 16).toInt()
        }
    }

    private fun createFlightCard(departureTime: String, priceModifier: Int): CardView {
        val cardView = layoutInflater.inflate(R.layout.item_flight_card, null) as CardView
        val arrivalTime = calculateArrivalTime(departureTime, boleto.duracion)

        cardView.findViewById<TextView>(R.id.tvDepartureTime).text = departureTime
        cardView.findViewById<TextView>(R.id.tvArrivalTime).text = arrivalTime
        cardView.findViewById<TextView>(R.id.tvDepartureCode).text = getAirportCode(boleto.origen)
        cardView.findViewById<TextView>(R.id.tvArrivalCode).text = getAirportCode(boleto.destino)
        cardView.findViewById<TextView>(R.id.tvConnection).text = "Conexión ${boleto.duracion}"

        val finalPrice = (basePrice + priceModifier).toInt()
        cardView.findViewById<Button>(R.id.btnPrice).apply {
            text = "$finalPrice €"
            setOnClickListener {
                selectedTime = departureTime
                val intent = if (isRoundTrip) {
                    // Creamos un boleto de vuelta invirtiendo origen y destino
                    val boletoVuelta = boleto.copy(
                        origen = boleto.destino,
                        destino = boleto.origen
                        // Puedes modificar duración, precio, etc., si son distintos
                    )

                    Intent(this@SeleccionarBoletoIdaActivity, SeleccionarBoletoVueltaActivity::class.java).apply {
                        putExtra("boletoIda", boleto)
                        putExtra("boletoVuelta", boletoVuelta)
                        putExtra("departureTime", departureTime)
                        putExtra("idaArrivalTime", arrivalTime)
                        putExtra("categoria", categoria)
                        putExtra("passengerCount", passengerCount)
                        putExtra("imageUrl", imageUrl)
                        putExtra("preferencias", preferenciasUsuario)
                        putExtra("idaPrecioSeleccionado", finalPrice) // Añadir el precio seleccionado
                        if (fechaInicio != null) putExtra("fechaInicio", fechaInicio!!.time)
                        if (fechaFin != null) putExtra("fechaFin", fechaFin!!.time)
                        if (selectedDate != null) putExtra("fechaIda", selectedDate!!.time)
                    }
                } else {
                    Intent(this@SeleccionarBoletoIdaActivity, TarifaActivity::class.java).apply {
                        putExtra("boletoIda", boleto)
                        putExtra("boletoVuelta", boleto) // Ensure both are set for consistency
                        putExtra("pricePerPassenger", boleto.precio.toDouble())
                        putExtra("precioTotal", finalPrice) // Add the selected final price
                        putExtra("departureTime", departureTime)
                        putExtra("idaDepartureTime", departureTime) // For consistency with naming
                        putExtra("idaArrivalTime", arrivalTime)
                        putExtra("categoria", categoria)
                        putExtra("passengerCount", passengerCount)
                        putExtra("imageUrl", imageUrl)
                        putExtra("preferencias", preferenciasUsuario)
                        putExtra("isRoundTrip", false)
                        if (selectedDate != null) putExtra("fechaIda", selectedDate!!.time)
                    }
                }
                startActivity(intent)
            }
        }

        val seats = (1..15).random()
        cardView.findViewById<TextView>(R.id.tvSeatsLeft)?.text =
            if (seats == 1) "¡Última plaza a este precio!" else "$seats plazas a este precio"

        return cardView
    }

    private fun calculateArrivalTime(departureTime: String, duration: String): String {
        val (h, m) = departureTime.split(":").map { it.toInt() }
        val pattern = "(\\d+)\\s*h(?:\\s*(\\d+)\\s*min)?".toRegex()
        val match = pattern.find(duration)
        val dh = match?.groups?.get(1)?.value?.toIntOrNull() ?: 0
        val dm = match?.groups?.get(2)?.value?.toIntOrNull() ?: 0
        return Calendar.getInstance().let { c ->
            c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m)
            c.add(Calendar.HOUR_OF_DAY, dh); c.add(Calendar.MINUTE, dm)
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(c.time)
        }
    }

    private fun getAirportCode(ciudad: String) = ciudad.take(3).uppercase()
}