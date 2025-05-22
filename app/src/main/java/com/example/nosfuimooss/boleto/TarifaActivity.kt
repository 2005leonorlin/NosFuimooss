package com.example.nosfuimooss.boleto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.NosFuimooss.R
import com.example.nosfuimooss.model.Boleto
import com.example.nosfuimooss.usuario.PreferenciasUsuario

class TarifaActivity : AppCompatActivity() {
    private lateinit var boletoIda: Boleto
    private lateinit var boletoVuelta: Boleto
    private lateinit var prefs: PreferenciasUsuario
    private var isRoundTrip = false
    private var pricePerPassenger: Double = 0.0
    private var precioTotal: Int = 0
    private var categoria = ""
    private var passengerCount = 1
    private var idaDeparture = ""
    private var idaArrival = ""
    private var vueltaDeparture = ""
    private var vueltaArrival = ""
    private var fechaIda: Long = 0L
    private var fechaVuelta: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarifa)
        supportActionBar?.hide()

        // Recuperar extras
        boletoIda         = intent.getSerializableExtra("boletoIda")   as Boleto
        boletoVuelta      = intent.getSerializableExtra("boletoVuelta")as? Boleto ?: boletoIda
        prefs             = intent.getSerializableExtra("preferencias")as? PreferenciasUsuario ?: PreferenciasUsuario(
            origen = TODO(),
            destino = TODO(),
            fechaInicio = TODO(),
            fechaFin = TODO(),
            cantidadPersonas = TODO(),
            categoria = TODO()
        )
        pricePerPassenger = intent.getDoubleExtra("pricePerPassenger", 0.0)
        precioTotal       = intent.getIntExtra("precioTotal", 0)
        categoria         = intent.getStringExtra("categoria").orEmpty()
        passengerCount    = intent.getIntExtra("passengerCount", 1)
        isRoundTrip       = intent.getBooleanExtra("isRoundTrip", false)
        idaDeparture      = intent.getStringExtra("idaDepartureTime") ?: intent.getStringExtra("departureTime").orEmpty()
        idaArrival        = intent.getStringExtra("idaArrivalTime").orEmpty()
        vueltaDeparture   = intent.getStringExtra("vueltaDepartureTime").orEmpty()
        vueltaArrival     = intent.getStringExtra("vueltaArrivalTime").orEmpty()
        fechaIda          = intent.getLongExtra("fechaIda", 0L)
        fechaVuelta       = intent.getLongExtra("fechaVuelta", 0L)

        // Mostrar información de debug en logs
        Log.d("TarifaActivity", "PrecioTotal recibido: $precioTotal")
        Log.d("TarifaActivity", "PricePerPassenger: $pricePerPassenger")
        Log.d("TarifaActivity", "isRoundTrip: $isRoundTrip")
        Log.d("TarifaActivity", "fechaIda: $fechaIda")
        Log.d("TarifaActivity", "fechaVuelta: $fechaVuelta")
        Log.d("TarifaActivity", "Horarios ida: $idaDeparture → $idaArrival")
        Log.d("TarifaActivity", "Horarios vuelta: $vueltaDeparture → $vueltaArrival")

        // Usar el precio total si está disponible
        val basePrice = if (precioTotal > 0) {
            precioTotal.toDouble()
        } else {
            // Calcular base + categoría como respaldo
            val catAdjustment = when(categoria) {
                "Turista Premium" -> 10.0
                "Business" -> 20.0
                "Primera Clase" -> 50.0
                else -> 0.0
            }
            (pricePerPassenger + catAdjustment) * passengerCount
        }

        findViewById<TextView>(R.id.tvRoute).text =
            if (isRoundTrip) "${boletoIda.origen} ↔ ${boletoIda.destino}"
            else "${boletoIda.origen} → ${boletoIda.destino}"

        findViewById<TextView>(R.id.tvTotalPrice).text =
            "Total (sin tarifa): ${basePrice.toInt()} €"

        setupFareCards(basePrice)
    }

    private fun setupFareCards(base: Double) {
        findViewById<CardView>(R.id.cardBasic).apply {
            findViewById<TextView>(R.id.tvBasicPrice).text =
                "${(base + 10).toInt()} €"
            findViewById<Button>(R.id.btnSelectBasic).setOnClickListener {
                goToDetalle("Básica", base + 10)
            }
        }
        findViewById<CardView>(R.id.cardStandard).apply {
            findViewById<TextView>(R.id.tvStandardPrice).text =
                "${(base + 20).toInt()} €"
            findViewById<Button>(R.id.btnSelectStandard).setOnClickListener {
                goToDetalle("Standard", base + 20)
            }
        }
        findViewById<CardView>(R.id.cardPremium).apply {
            findViewById<TextView>(R.id.tvPremiumPrice).text =
                "${(base + 50).toInt()} €"
            findViewById<Button>(R.id.btnSelectPremium).setOnClickListener {
                goToDetalle("Premium", base + 50)
            }
        }
    }

    private fun goToDetalle(fareType: String, totalPrice: Double) {
        Intent(this, DetalleBoletoActivity::class.java).apply {
            putExtra("boletoIda", boletoIda)
            putExtra("boletoVuelta", boletoVuelta)
            putExtra("preferencias", prefs)
            putExtra("imageUrl", intent.getStringExtra("imageUrl"))
            putExtra("isRoundTrip", isRoundTrip)
            putExtra("fareType", fareType)
            putExtra("totalPrice", totalPrice)

            // Ida
            putExtra("idaDepartureTime", idaDeparture)
            putExtra("idaArrivalTime", idaArrival)
            putExtra("fechaIda", fechaIda)

            // Vuelta - ASEGÚRATE DE PASAR LA FECHA DE VUELTA SI ES isRoundTrip
            if (isRoundTrip) {
                putExtra("vueltaDepartureTime", vueltaDeparture)
                putExtra("vueltaArrivalTime", vueltaArrival)
                putExtra("fechaVuelta", fechaVuelta)

                // Log para verificar que estamos enviando fechaVuelta correctamente
                Log.d("TarifaActivity", "Enviando fechaVuelta: $fechaVuelta a DetalleBoletoActivity")
            }
        }.also { startActivity(it) }
    }
}