package com.example.nosfuimooss.boleto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.NosFuimooss.R

import com.example.nosfuimooss.model.Boleto
import com.example.nosfuimooss.model.PreferenciasUsuario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalleBoletoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_boleto)
        supportActionBar?.hide()

        val ivDestino    = findViewById<ImageView>(R.id.ivDestino)
        val tvRuta       = findViewById<TextView>(R.id.tvRuta)
        val tvTipo       = findViewById<TextView>(R.id.tvTipoTrayecto)
        val tvFechas     = findViewById<TextView>(R.id.tvFechas)
        val tvHorarios   = findViewById<TextView>(R.id.tvHorarios)
        val tvClase      = findViewById<TextView>(R.id.tvClase)
        val tvPersonas   = findViewById<TextView>(R.id.tvPersonas)
        val tvTarifa     = findViewById<TextView>(R.id.tvTarifa)
        val tvPrecio     = findViewById<TextView>(R.id.tvPrecioTotal)
        val btnComprar   = findViewById<Button>(R.id.btnComprar)
        val btnCancelar  = findViewById<Button>(R.id.btnCancelar)

        // Recuperar extras
        val boletoIda    = intent.getSerializableExtra("boletoIda")     as Boleto
        val boletoVuelta = intent.getSerializableExtra("boletoVuelta")  as Boleto
        val prefs        = intent.getSerializableExtra("preferencias")  as PreferenciasUsuario
        val imageUrl     = intent.getStringExtra("imageUrl").orEmpty()
        val isRT         = intent.getBooleanExtra("isRoundTrip", false)
        val fareType     = intent.getStringExtra("fareType").orEmpty()
        val totalPrice   = intent.getDoubleExtra("totalPrice", 0.0)

        val idaDep   = intent.getStringExtra("idaDepartureTime").orEmpty()
        val idaArr   = intent.getStringExtra("idaArrivalTime").orEmpty()
        val vueltaDep= intent.getStringExtra("vueltaDepartureTime").orEmpty()
        val vueltaArr= intent.getStringExtra("vueltaArrivalTime").orEmpty()

        val fechaIdaMs   = intent.getLongExtra("fechaIda", 0L)
        val fechaVuelMs  = intent.getLongExtra("fechaVuelta", 0L)

        // Log para verificar que estamos recibiendo la fecha de vuelta
        Log.d("DetalleBoletoActivity", "fechaIda: ${formatDateForLog(fechaIdaMs)}")
        Log.d("DetalleBoletoActivity", "fechaVuelta: ${formatDateForLog(fechaVuelMs)}")
        Log.d("DetalleBoletoActivity", "isRoundTrip: $isRT")

        // Cargar imagen
        Glide.with(this)
            .load(imageUrl)
            .into(ivDestino)

        // Origen → Destino y tipo
        tvRuta.text = "${prefs.origen} → ${prefs.destino}"
        tvTipo.text = if (isRT) "Ida y Vuelta" else "Solo Ida"

        // Fechas - Aquí está el cambio principal
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val fechaIda = if (fechaIdaMs > 0) fmt.format(Date(fechaIdaMs)) else fmt.format(Date(prefs.fechaInicio))

        tvFechas.text = if (isRT && fechaVuelMs > 0L) {
            // Si es ida y vuelta Y tenemos una fecha de vuelta válida
            "Ida: $fechaIda\nVuelta: ${fmt.format(Date(fechaVuelMs))}"
        } else if (isRT) {
            // Si es ida y vuelta pero no tenemos fecha de vuelta, usamos fechaFin de preferencias
            "Ida: $fechaIda\nVuelta: ${fmt.format(Date(prefs.fechaFin))}"
        } else {
            // Solo ida
            "Fecha: $fechaIda"
        }

        // Horarios con llegada
        val sb = StringBuilder()
            .append("Ida: $idaDep → $idaArr")
        if (isRT) {
            sb.append("\nVuelta: $vueltaDep → $vueltaArr")
        }
        tvHorarios.text = sb.toString()

        // Clase, personas, tarifa y precio
        tvClase.text     = prefs.categoria
        tvPersonas.text  = "${prefs.cantidadPersonas} persona(s)"
        tvTarifa.text    = fareType
        tvPrecio.text    = "Total: ${totalPrice.toInt()} €"

        // Botones
        val ci = Intent(this, DatosPasajerosActivity::class.java)
        ci.putExtra("boletoIda", boletoIda)
        ci.putExtra("boletoVuelta", boletoVuelta)
        ci.putExtra("preferencias", prefs)
        ci.putExtra("imageUrl", imageUrl)
        ci.putExtra("isRoundTrip", isRT)
        ci.putExtra("fareType", fareType)
        ci.putExtra("totalPrice", totalPrice)
        ci.putExtra("idaDepartureTime", idaDep)
        ci.putExtra("idaArrivalTime", idaArr)

        // Asegúrate de que estos datos se pasen correctamente en caso de ida y vuelta
        ci.putExtra("fechaIda", fechaIdaMs)

        if (isRT) {
            ci.putExtra("vueltaDepartureTime", vueltaDep)
            ci.putExtra("vueltaArrivalTime", vueltaArr)
            ci.putExtra("fechaVuelta", fechaVuelMs) // Asegúrate de que esta variable tenga valor

            // Agrega un log para verificar el valor
            Log.d("DetalleBoleto", "Pasando fechaVuelta a DatosPasajeros: $fechaVuelMs")
        }
        startActivity(ci)
    }

    private fun formatDateForLog(timestamp: Long): String {
        return if (timestamp > 0) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
        } else {
            "No disponible (0)"
        }
    }
}