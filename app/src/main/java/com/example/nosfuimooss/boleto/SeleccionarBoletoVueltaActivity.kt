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
import com.example.nosfuimooss.model.PreferenciasUsuario
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SeleccionarBoletoVueltaActivity : AppCompatActivity() {
    private lateinit var tvRoute: TextView
    private lateinit var tvPassengerInfo: TextView
    private lateinit var datesContainer: LinearLayout
    private lateinit var dateScroll: HorizontalScrollView
    private var imageUrl: String = ""
    private lateinit var preferencias: PreferenciasUsuario
    private lateinit var boletoIda: Boleto
    private lateinit var boletoVuelta: Boleto
    private var fechaInicio: Date? = null
    private var fechaFin: Date? = null
    private var fechaIda: Long = 0L
    private var selectedDate: Date? = null
    private var basePrice: Double = 0.0
    private var categoria: String = ""
    private var passengerCount: Int = 1
    private var departureTimeIda: String = ""
    private var arrivalTimeIda: String = ""

    private val timePriceList = listOf(
        "06:30" to 50,
        "11:45" to 60,
        "13:30" to 75,
        "16:30" to 85,
        "18:50" to 90,
        "20:30" to 100
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccionar_boleto_vuelta)
        supportActionBar?.hide()

        // Views
        tvRoute = findViewById(R.id.tvRoute)
        tvPassengerInfo = findViewById(R.id.tvPassengerInfo)
        dateScroll = findViewById(R.id.dateScroll)
        datesContainer = dateScroll.getChildAt(0) as LinearLayout

        // Extras
        boletoIda = intent.getSerializableExtra("boletoIda") as Boleto
        boletoVuelta = intent.getSerializableExtra("boletoVuelta") as? Boleto ?: boletoIda
        preferencias = intent.getSerializableExtra("preferencias") as PreferenciasUsuario
        imageUrl = intent.getStringExtra("imageUrl").orEmpty()
        departureTimeIda = intent.getStringExtra("departureTime").orEmpty()
        arrivalTimeIda = intent.getStringExtra("idaArrivalTime").orEmpty()

        // Recuperar el precio seleccionado en la pantalla de ida
        val idaPrecioSeleccionado = intent.getIntExtra("idaPrecioSeleccionado", 0)

        fechaInicio = Date(intent.getLongExtra("fechaInicio", 0L))
        fechaFin = Date(intent.getLongExtra("fechaFin", 0L))
        fechaIda = intent.getLongExtra("fechaIda", 0L)

        categoria = intent.getStringExtra("categoria").orEmpty()
        passengerCount = intent.getIntExtra("passengerCount", 1)

        // UI
        tvRoute.text = "${boletoVuelta.destino} - ${boletoVuelta.origen}"
        val pasajeros = if (passengerCount == 1) "1 adulto" else "$passengerCount adultos"
        tvPassengerInfo.text = "Vuelo de regreso · $pasajeros · $categoria"

        // Usar el precio seleccionado de ida para calcular el precio base de vuelta
        // Si idaPrecioSeleccionado es 0, usar el cálculo original
        basePrice = if (idaPrecioSeleccionado > 0) {
            idaPrecioSeleccionado.toDouble() // Usar directamente el precio seleccionado
        } else {
            (boletoVuelta.precio + when (categoria) {
                "Turista Premium" -> 10
                "Business" -> 20
                "Primera Clase" -> 50
                else -> 1
            }) * passengerCount
        }

        Log.d("VUELTA", "Ida precio seleccionado: $idaPrecioSeleccionado")
        Log.d("VUELTA", "Ida: dep=$departureTimeIda, arr=$arrivalTimeIda")
        Log.d("VUELTA", "BasePrice=$basePrice, Categoria=$categoria")

        setupDateScroller()
    }

    private fun setupDateScroller() {
        datesContainer.removeAllViews()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = sdf.parse(boletoVuelta.fechaInicial) ?: return
        val endDate = sdf.parse(boletoVuelta.fechaFinal) ?: return

        // Por defecto, selecionamos fechaFin como fecha de vuelta
        selectedDate = fechaFin

        val diffInMillis = endDate.time - startDate.time
        val daysDiff = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS).toInt() + 1

        val calendar = Calendar.getInstance().apply { time = startDate }
        val displayFormat = SimpleDateFormat("EEE dd MMM", Locale("es", "ES"))

        for (i in 0 until daysDiff) {
            val date = calendar.time
            val isSelected = selectedDate?.let { sameDay(it, date) } == true
            val view = createDateView(displayFormat.format(date), isSelected)
            view.setOnClickListener {
                clearDateSelection()
                applyDateSelection(view)
                selectedDate = date
                setupFlightOptions()
            }
            datesContainer.addView(view)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        setupFlightOptions()
    }

    private fun clearDateSelection() {
        for (i in 0 until datesContainer.childCount) {
            val view = datesContainer.getChildAt(i)
            view.setBackgroundColor(resources.getColor(android.R.color.transparent))
            val tv = (view as LinearLayout).getChildAt(0) as TextView
            tv.setTextColor(resources.getColor(android.R.color.darker_gray))
            tv.textSize = 16f
            if (view.childCount > 1) view.removeViewAt(1)
        }
    }

    private fun applyDateSelection(view: LinearLayout) {
        view.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
        val tv = view.getChildAt(0) as TextView
        tv.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        tv.textSize = 16f
        val indicator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (resources.displayMetrics.density * 60).toInt(),
                (resources.displayMetrics.density * 3).toInt()
            ).also { it.topMargin = (resources.displayMetrics.density * 8).toInt() }
            setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
        }
        view.addView(indicator)
    }

    private fun createDateView(dateText: String, isSelected: Boolean): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }

        val textView = TextView(this).apply {
            text = dateText
            textSize = 16f
            setTextColor(
                if (isSelected) resources.getColor(android.R.color.holo_green_dark)
                else resources.getColor(android.R.color.darker_gray)
            )
        }

        layout.addView(textView)

        if (isSelected) {
            layout.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
            val indicator = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (resources.displayMetrics.density * 60).toInt(),
                    (resources.displayMetrics.density * 3).toInt()
                ).also { it.topMargin = (resources.displayMetrics.density * 8).toInt() }
                setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            }
            layout.addView(indicator)
        }

        return layout
    }

    private fun setupFlightOptions() {
        val container = findViewById<LinearLayout>(R.id.flightOptionsContainer)
        container.removeAllViews()

        val options = timePriceList.shuffled().take(3)
        for ((time, priceExtra) in options) {
            val card = layoutInflater.inflate(R.layout.item_flight_card, null) as CardView
            val arrival = calculateArrivalTime(time, boletoVuelta.duracion)

            card.findViewById<TextView>(R.id.tvDepartureTime).text = time
            card.findViewById<TextView>(R.id.tvArrivalTime).text = arrival
            card.findViewById<TextView>(R.id.tvDepartureCode).text = getAirportCode(boletoVuelta.origen)
            card.findViewById<TextView>(R.id.tvArrivalCode).text = getAirportCode(boletoVuelta.destino)
            card.findViewById<TextView>(R.id.tvConnection).text = "Conexión ${boletoVuelta.duracion}"

            // El precio de vuelta ahora se basa correctamente en basePrice que ya incorpora el precio de ida
            val finalPrice = (basePrice + priceExtra).toInt()

            card.findViewById<Button>(R.id.btnPrice).apply {
                text = "$finalPrice €"
                setOnClickListener {
                    val intent = Intent(this@SeleccionarBoletoVueltaActivity, TarifaActivity::class.java)
                    // Enviamos correctamente Ida y Vuelta
                    intent.putExtra("boletoIda", boletoIda)
                    intent.putExtra("boletoVuelta", boletoVuelta)
                    intent.putExtra("preferencias", preferencias)
                    intent.putExtra("imageUrl", imageUrl)
                    intent.putExtra("isRoundTrip", true)
                    intent.putExtra("pricePerPassenger", boletoVuelta.precio.toDouble())
                    intent.putExtra("precioTotal", finalPrice) // Transferir el precio total calculado

                    intent.putExtra("categoria", categoria)
                    intent.putExtra("passengerCount", passengerCount)

                    // Horarios
                    intent.putExtra("idaDepartureTime", departureTimeIda)
                    intent.putExtra("idaArrivalTime", arrivalTimeIda)
                    intent.putExtra("vueltaDepartureTime", time)
                    intent.putExtra("vueltaArrivalTime", arrival)


                    // Fechas
                    intent.putExtra("fechaIda", fechaIda)
                    intent.putExtra("fechaVuelta", selectedDate?.time)
                    startActivity(intent)
                }
            }

            container.addView(card)
            (card.layoutParams as LinearLayout.LayoutParams).bottomMargin =
                (resources.displayMetrics.density * 16).toInt()
        }
    }

    private fun calculateArrivalTime(departure: String, duracion: String): String {
        val (h, m) = departure.split(":").map { it.toInt() }
        val regex = "(\\d+)\\s*h(?:\\s*(\\d+)\\s*min)?".toRegex()
        val match = regex.find(duracion)
        val dh = match?.groups?.get(1)?.value?.toIntOrNull() ?: 0
        val dm = match?.groups?.get(2)?.value?.toIntOrNull() ?: 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            add(Calendar.HOUR_OF_DAY, dh)
            add(Calendar.MINUTE, dm)
        }

        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
    }

    private fun sameDay(d1: Date, d2: Date): Boolean {
        val c1 = Calendar.getInstance().apply { time = d1 }
        val c2 = Calendar.getInstance().apply { time = d2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                && c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH)
    }

    private fun getAirportCode(ciudad: String): String = ciudad.take(3).uppercase()
}
