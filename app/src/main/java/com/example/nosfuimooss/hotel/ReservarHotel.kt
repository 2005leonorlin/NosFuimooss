package com.example.nosfuimooss.hotel

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.NosFuimooss.R
import com.example.nosfuimooss.Adapter.HotelAdapter
import com.example.nosfuimooss.api.RetrofitClient
import com.example.nosfuimooss.model.DestinoInfo
import com.example.nosfuimooss.model.Hotel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class ReservarHotel : AppCompatActivity() {
    private lateinit var etDestino: EditText
    private lateinit var tvFechaEntrada: TextView
    private lateinit var tvFechaSalida: TextView
    private lateinit var tvAdultos: TextView
    private lateinit var tvNinos: TextView
    private lateinit var tvHabitaciones: TextView
    private lateinit var btnOrdenar: Button
    private lateinit var btnMapa: Button
    private lateinit var btnBuscarHoteles: Button
    private lateinit var rvHoteles: RecyclerView
    private lateinit var tvResultsCount: TextView

    private var listaHoteles: List<Hotel> = listOf()
    private var listaHotelesFiltrada: List<Hotel> = listOf()
    private lateinit var hotelAdapter: HotelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservar_hotel)

        // Referencias
        etDestino = findViewById(R.id.etDestino)
        tvFechaEntrada = findViewById(R.id.tvFechaEntrada)
        tvFechaSalida = findViewById(R.id.tvFechaSalida)
        tvAdultos = findViewById(R.id.tvAdultos)
        tvNinos = findViewById(R.id.tvNinos)
        tvHabitaciones = findViewById(R.id.tvHabitaciones)
        btnOrdenar = findViewById(R.id.btnOrdenar)

        btnBuscarHoteles = findViewById(R.id.btnBuscarHoteles)
        rvHoteles = findViewById(R.id.rvHoteles)
        tvResultsCount = findViewById(R.id.tvResultsCount)

        // Configurar RecyclerView
        rvHoteles.layoutManager = LinearLayoutManager(this)
        hotelAdapter = HotelAdapter(listOf())
        rvHoteles.adapter = hotelAdapter

        // Configurar button clear para el destino
        findViewById<ImageButton>(R.id.btnClearDestino).setOnClickListener {
            etDestino.setText("")
        }

        // Setear fecha
        tvFechaEntrada.setOnClickListener { mostrarSelectorFecha(tvFechaEntrada) }
        tvFechaSalida.setOnClickListener { mostrarSelectorFecha(tvFechaSalida) }

        // Contadores
        setUpContador(R.id.btnIncreaseAdultos, R.id.btnDecreaseAdultos, tvAdultos, min = 1)
        setUpContador(R.id.btnIncreaseNinos, R.id.btnDecreaseNinos, tvNinos, min = 0)
        setUpContador(R.id.btnIncreaseHabitaciones, R.id.btnDecreaseHabitaciones, tvHabitaciones, min = 1)

        // Recibir ID del vuelo
        val idVuelo = intent.getStringExtra("ID_VUELO")
        if (idVuelo != null) {
            cargarDatosDelDestino(idVuelo)
        }

        // Botón para ordenar hoteles
        btnOrdenar.setOnClickListener {
            mostrarOpcionesOrden()
        }

        // Botón para búsqueda de hoteles
        btnBuscarHoteles.setOnClickListener {
            buscarHoteles()
        }

        // Botón para abrir el mapa
        btnMapa.setOnClickListener {
            mostrarMapa()
        }
    }

    private fun mostrarSelectorFecha(targetView: TextView) {
        val calendar = Calendar.getInstance()

        // Si ya hay una fecha, la usamos como fecha inicial
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val textoFecha = targetView.text.toString()

        if (textoFecha != getString(R.string.fecha_por_defecto)) {
            try {
                val date = dateFormat.parse(textoFecha)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                // Si hay error al parsear, usamos la fecha actual
            }
        }

        val dialog = DatePickerDialog(this,
            { _, year, month, day ->
                val fecha = "%02d/%02d/%04d".format(day, month + 1, year)
                targetView.text = fecha
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Configurar fecha mínima (hoy)
        dialog.datePicker.minDate = Calendar.getInstance().timeInMillis

        dialog.show()
    }

    private fun setUpContador(btnUpId: Int, btnDownId: Int, textView: TextView, min: Int) {
        findViewById<ImageButton>(btnUpId).setOnClickListener {
            val actual = textView.text.toString().toInt()
            textView.text = (actual + 1).toString()
        }
        findViewById<ImageButton>(btnDownId).setOnClickListener {
            val actual = textView.text.toString().toInt()
            if (actual > min) textView.text = (actual - 1).toString()
        }
    }

    private fun cargarDatosDelDestino(idVuelo: String) {
        RetrofitClient.destinoApiService.getInfoPorIdVuelo(idVuelo)
            .enqueue(object : Callback<DestinoInfo> {
                override fun onResponse(call: Call<DestinoInfo>, response: Response<DestinoInfo>) {
                    if (response.isSuccessful) {
                        val destinoInfo = response.body()
                        val destino = destinoInfo?.vuelo?.nombre ?: ""

                        etDestino.setText(destino)
                        // No deshabilitamos el campo para permitir edición
                        // etDestino.isEnabled = false

                        listaHoteles = destinoInfo?.hoteles ?: listOf()
                        listaHotelesFiltrada = listaHoteles
                        actualizarResultados()
                    }
                }

                override fun onFailure(call: Call<DestinoInfo>, t: Throwable) {
                    Toast.makeText(this@ReservarHotel, "Error al conectar: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun buscarHoteles() {
        val destino = etDestino.text.toString().trim()

        if (destino.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese un destino", Toast.LENGTH_SHORT).show()
            return
        }

        val fechaEntrada = tvFechaEntrada.text.toString()
        val fechaSalida = tvFechaSalida.text.toString()

        if (fechaEntrada == getString(R.string.fecha_por_defecto) ||
            fechaSalida == getString(R.string.fecha_por_defecto)) {
            Toast.makeText(this, "Por favor, seleccione fechas de entrada y salida", Toast.LENGTH_SHORT).show()
            return
        }

        // Consultar hoteles en la ubicación
        RetrofitClient.hotelApiService.getHotelesPorUbicacion(destino)
            .enqueue(object : Callback<List<Hotel>> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<List<Hotel>>, response: Response<List<Hotel>>) {
                    if (response.isSuccessful) {
                        listaHoteles = response.body() ?: listOf()
                        filtrarHotelesPorFecha(fechaEntrada, fechaSalida)
                    } else {
                        Toast.makeText(this@ReservarHotel, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<Hotel>>, t: Throwable) {
                    Toast.makeText(this@ReservarHotel, "Error al conectar: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filtrarHotelesPorFecha(fechaEntrada: String, fechaSalida: String) {
        val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val fechaEntradaDate = try {
            LocalDate.parse(fechaEntrada, dateFormat)
        } catch (e: Exception) {
            null
        }

        val fechaSalidaDate = try {
            LocalDate.parse(fechaSalida, dateFormat)
        } catch (e: Exception) {
            null
        }

        // Filtrar por fechas disponibles
        listaHotelesFiltrada = if (fechaEntradaDate != null && fechaSalidaDate != null) {
            listaHoteles.filter { hotel ->
                val fechaInicioHotel = hotel.fechaDisponible?.inicio
                val fechaFinHotel = hotel.fechaDisponible?.fin

                // Si el hotel tiene fecha disponible, verificamos que esté en el rango
                if (fechaInicioHotel != null && fechaFinHotel != null) {
                    fechaEntradaDate >= fechaInicioHotel && fechaSalidaDate <= fechaFinHotel
                } else {
                    // Si no tiene fecha especificada, lo incluimos
                    true
                }
            }
        } else {
            listaHoteles
        }

        // Aplicar ajustes de precio según adultos, niños y habitaciones
        ajustarPrecios()

        // Actualizar la UI
        actualizarResultados()
    }

    private fun ajustarPrecios() {
        val adultos = tvAdultos.text.toString().toInt()
        val ninos = tvNinos.text.toString().toInt()
        val habitaciones = tvHabitaciones.text.toString().toInt()

        // Crear una lista de hoteles con precios ajustados
        listaHotelesFiltrada = listaHotelesFiltrada.map { hotel ->
            val precioBase = hotel.precioNoche
            val precioAjustado = precioBase +
                    (adultos - 1) * 20.0 +  // El primer adulto ya está incluido
                    ninos * 10.0 +
                    (habitaciones - 1) * 50.0  // La primera habitación ya está incluida

            // Crear una copia del hotel con el precio ajustado
            Hotel(
                id = hotel.id,
                nombre = hotel.nombre,
                ubicacion = hotel.ubicacion,
                precioNoche = precioAjustado,
                disponible = hotel.disponible,
                imagen = hotel.imagen,
                estrellas = hotel.estrellas,
                fechaDisponible = hotel.fechaDisponible
            )
        }
    }

    private fun actualizarResultados() {
        // Actualizar contador de resultados
        tvResultsCount.text = "${listaHotelesFiltrada.size} hoteles encontrados"

        // Actualizar RecyclerView
        hotelAdapter.updateList(listaHotelesFiltrada)
    }

    private fun mostrarOpcionesOrden() {
        val opciones = arrayOf("Precio (menor a mayor)", "Precio (mayor a menor)", "Estrellas (mayor a menor)", "Estrellas (menor a mayor)")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ordenar por:")
        builder.setItems(opciones) { _, which ->
            listaHotelesFiltrada = when (which) {
                0 -> listaHotelesFiltrada.sortedBy { it.precioNoche }
                1 -> listaHotelesFiltrada.sortedByDescending { it.precioNoche }
                2 -> listaHotelesFiltrada.sortedByDescending { it.estrellas }
                3 -> listaHotelesFiltrada.sortedBy { it.estrellas }
                else -> listaHotelesFiltrada
            }
            actualizarResultados()
        }
        builder.show()
    }

    private fun mostrarMapa() {
        val intent = Intent(this, MapActivity::class.java)


        startActivity(intent)
    }
}