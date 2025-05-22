package com.example.nosfuimooss.hotel

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Filter

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button

import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.NosFuimooss.R
import com.example.nosfuimooss.Adapter.HotelAdapter
import com.example.nosfuimooss.api.RetrofitClient

import com.example.nosfuimooss.model.Hotel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class ReservarHotel : AppCompatActivity() {
    private lateinit var etDestino: AutoCompleteTextView
    private lateinit var btnClearDestino: ImageButton
    private lateinit var btnBuscarHoteles: Button
    private lateinit var rvHoteles: RecyclerView
    private lateinit var tvResultsCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvFechaEntrada: TextView
    private lateinit var tvFechaSalida: TextView
    private lateinit var tvNoches: TextView
    private var fechaEntrada: Date? = null
    private var fechaSalida: Date? = null
    private var adultos = 1
    private var ninos = 0
    private lateinit var adapter: HotelAdapter
    private var allHoteles: List<Hotel> = emptyList()
    private var ubicacionesDisponibles: MutableSet<String> = mutableSetOf()

    // Variable para controlar el retraso en filtrado
    private var filterTextChangedJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_reservar_hotel)

        etDestino = findViewById(R.id.etDestino)
        btnClearDestino = findViewById(R.id.btnClearDestino)
        btnBuscarHoteles = findViewById(R.id.btnBuscarHoteles)
        rvHoteles = findViewById(R.id.rvHoteles)
        tvResultsCount = findViewById(R.id.tvResultsCount)
        progressBar = findViewById(R.id.progressBar)
        tvNoches = findViewById(R.id.tvNoches)
        tvFechaEntrada = findViewById(R.id.tvFechaEntrada)
        tvFechaSalida = findViewById(R.id.tvFechaSalida)

        tvFechaEntrada.setOnClickListener {
            mostrarDatePicker(true)
        }

        tvFechaSalida.setOnClickListener {
            mostrarDatePicker(false)
        }

        etDestino.threshold = 1
        rvHoteles.layoutManager = LinearLayoutManager(this)

        // Aquí cambiamos el lambda para abrir la actividad de detalle en lugar de mostrar un Toast
        adapter = HotelAdapter(emptyList(), { hotel ->
            // Calculamos el precio final con el número actual de personas
            val totalPersonas = adultos + ninos
            val adicionales = (totalPersonas - 2).coerceAtLeast(0)
            val precioFinal = hotel.precioNoche + (adicionales * 22)

            // Creamos un intent para ir a DetalleHotelActivity
            val intent = Intent(this, DetalleHotelActivity::class.java).apply {
                putExtra("hotel", hotel)
                putExtra("adultos", adultos)
                putExtra("ninos", ninos)
                putExtra("destino", etDestino.text.toString())
                putExtra("fechaEntrada", tvFechaEntrada.text.toString())
                putExtra("fechaSalida", tvFechaSalida.text.toString())
                putExtra("precioFinal", precioFinal)
            }

            // Iniciamos la actividad
            startActivity(intent)
        }, adultos, ninos)

        rvHoteles.adapter = adapter
        findViewById<ImageButton>(R.id.btnIncreaseAdultos).setOnClickListener {
            adultos++
            findViewById<TextView>(R.id.tvAdultos).text = adultos.toString()
            adapter.actualizarCantidadPersonas(adultos, ninos)
        }

        findViewById<ImageButton>(R.id.btnDecreaseAdultos).setOnClickListener {
            if (adultos > 1) {
                adultos--
                findViewById<TextView>(R.id.tvAdultos).text = adultos.toString()
                adapter.actualizarCantidadPersonas(adultos, ninos)
            }
        }

        // Botones para niños
        findViewById<ImageButton>(R.id.btnIncreaseNinos).setOnClickListener {
            ninos++
            findViewById<TextView>(R.id.tvNinos).text = ninos.toString()
            adapter.actualizarCantidadPersonas(adultos, ninos)
        }

        findViewById<ImageButton>(R.id.btnDecreaseNinos).setOnClickListener {
            if (ninos > 0) {
                ninos--
                findViewById<TextView>(R.id.tvNinos).text = ninos.toString()
                adapter.actualizarCantidadPersonas(adultos, ninos)
            }
        }
        findViewById<Button>(R.id.btnMapa).setOnClickListener {
            // Mostrar indicador de carga
            progressBar.visibility = View.VISIBLE

            // Usar lifecycleScope para preparar datos en segundo plano
            lifecycleScope.launch {
                val hotelesFiltrados = withContext(Dispatchers.Default) {
                    adapter.getHoteles()
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    // Comprobar si tenemos hoteles con coordenadas
                    val hotelesConCoordenadas = hotelesFiltrados.filter {
                        it.latitud != null && it.longitud != null
                    }

                    if (hotelesConCoordenadas.isEmpty()) {
                        Toast.makeText(
                            this@ReservarHotel,
                            "No hay hoteles con coordenadas para mostrar en el mapa",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@withContext
                    }

                    // Iniciar la actividad del mapa con los datos filtrados
                    val intent = Intent(this@ReservarHotel, MapaActivity::class.java)
                    intent.putExtra("hoteles", ArrayList(hotelesFiltrados))

                    // Pasar datos adicionales del usuario
                    intent.putExtra("adultos", adultos)
                    intent.putExtra("ninos", ninos)
                    intent.putExtra("destino", etDestino.text.toString())
                    intent.putExtra("fechaEntrada", tvFechaEntrada.text.toString())
                    intent.putExtra("fechaSalida", tvFechaSalida.text.toString())

                    startActivity(intent)
                }
            }
        }

        // Capitalizar automáticamente cada palabra al escribir con manejo de corrutinas
        etDestino.addTextChangedListener(object : TextWatcher {
            private var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isEditing || s == null) return

                val capitalized = s.toString()
                    .lowercase()
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

                if (s.toString() != capitalized) {
                    isEditing = true
                    etDestino.setText(capitalized)
                    etDestino.setSelection(capitalized.length)
                    isEditing = false
                }

                if (capitalized.isNotEmpty()) {
                    btnClearDestino.visibility = View.VISIBLE

                    // Cancelar el trabajo anterior si existe
                    filterTextChangedJob?.cancel()

                    // Iniciar un nuevo trabajo con retraso para evitar filtrados excesivos
                    filterTextChangedJob = lifecycleScope.launch {
                        kotlinx.coroutines.delay(300) // Pequeño retraso para evitar filtrar en cada pulsación
                        filtrarYMostrarHoteles(capitalized)
                    }
                } else {
                    btnClearDestino.visibility = View.GONE
                    lifecycleScope.launch(Dispatchers.Main) {
                        adapter.updateData(allHoteles)
                        tvResultsCount.text = "Se encontraron ${allHoteles.size} hoteles"
                    }
                }
            }
        })

        btnBuscarHoteles.setOnClickListener {
            val destino = etDestino.text.toString().trim()
            if (destino.isEmpty()) {
                Toast.makeText(this, "Por favor, introduce un destino", Toast.LENGTH_SHORT).show()
            } else {
                buscarHotelesPorDestino(destino)
            }
        }

        btnClearDestino.setOnClickListener {
            etDestino.text.clear()
            lifecycleScope.launch(Dispatchers.Main) {
                adapter.updateData(allHoteles)
                tvResultsCount.text = "Se encontraron ${allHoteles.size} hoteles"
            }
        }
        val btnOrdenar: Button = findViewById(R.id.btnOrdenar)
        btnOrdenar.setOnClickListener {
            mostrarOpcionesOrden()
        }

        cargarTodosLosHoteles()

    }

    private fun mostrarOpcionesOrden() {
        val opciones = arrayOf("Precio", "Estrellas")
        AlertDialog.Builder(this)
            .setTitle("Ordenar hoteles por")
            .setItems(opciones) { _, which ->
                lifecycleScope.launch {
                    val hotelesOrdenados = withContext(Dispatchers.Default) {
                        when (which) {
                            0 -> { // Precio ascendente
                                allHoteles.sortedBy { it.precioNoche }
                            }
                            1 -> { // Estrellas descendente
                                allHoteles.sortedByDescending { it.estrellas }
                            }
                            else -> allHoteles
                        }
                    }

                    withContext(Dispatchers.Main) {
                        allHoteles = hotelesOrdenados
                        adapter.updateData(allHoteles)
                        tvResultsCount.text = "Se encontraron ${allHoteles.size} hoteles"
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDatePicker(esEntrada: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            val selectedDate = calendar.time
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = dateFormat.format(selectedDate)

            if (esEntrada) {
                fechaEntrada = selectedDate
                tvFechaEntrada.text = formattedDate

                // Si la salida es anterior, igualar fechas
                if (fechaSalida != null && fechaSalida!! < fechaEntrada!!) {
                    fechaSalida = selectedDate
                    tvFechaSalida.text = formattedDate
                }
            } else {
                if (fechaEntrada != null && selectedDate < fechaEntrada!!) {
                    Toast.makeText(this, "La fecha de salida debe ser posterior a la de entrada", Toast.LENGTH_SHORT).show()
                    return@DatePickerDialog
                }
                fechaSalida = selectedDate
                tvFechaSalida.text = formattedDate
            }
            actualizarNumeroDeNoches()

        }, year, month, day)

        // No permitir seleccionar fechas pasadas
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()

        datePickerDialog.show()

    }

    private fun actualizarNumeroDeNoches() {
        if (fechaEntrada != null && fechaSalida != null) {
            val diferenciaMillis = fechaSalida!!.time - fechaEntrada!!.time
            val noches = (diferenciaMillis / (1000 * 60 * 60 * 24)).toInt()

            if (noches >= 1) {
                tvNoches.text = "Estancia de $noches noche${if (noches > 1) "s" else ""}"
            } else {
                tvNoches.text = ""
            }
        } else {
            tvNoches.text = ""
        }
    }

    private fun cargarTodosLosHoteles() {
        progressBar.visibility = View.VISIBLE
        tvResultsCount.text = "Cargando hoteles..."

        RetrofitClient.hotelApiService.getAllHoteles().enqueue(object : Callback<List<Hotel>> {
            override fun onResponse(call: Call<List<Hotel>>, response: Response<List<Hotel>>) {
                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    lifecycleScope.launch {
                        val hoteles = response.body().orEmpty()

                        // Procesar datos en segundo plano
                        withContext(Dispatchers.Default) {
                            allHoteles = hoteles
                            ubicacionesDisponibles = allHoteles.map { it.ubicacion }.toMutableSet()
                        }

                        // Actualizar UI en hilo principal
                        withContext(Dispatchers.Main) {
                            adapter.updateData(allHoteles)
                            tvResultsCount.text = "Se encontraron ${allHoteles.size} hoteles"
                            configureAutoComplete()

                            val ubicacionPreseleccionada = intent.getStringExtra("ubicacionDestino")
                            if (!ubicacionPreseleccionada.isNullOrEmpty()) {
                                etDestino.setText(ubicacionPreseleccionada)
                                buscarHotelesPorDestino(ubicacionPreseleccionada)
                            }
                        }
                    }
                } else {
                    Log.e("ReservarHotel", "Error: ${response.code()} - ${response.message()}")
                    Toast.makeText(
                        this@ReservarHotel,
                        "Error al cargar hoteles: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    tvResultsCount.text = "Error al cargar hoteles"
                }
            }

            override fun onFailure(call: Call<List<Hotel>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.e("ReservarHotel", "Error de red: ${t.message}", t)
                Toast.makeText(
                    this@ReservarHotel,
                    "Error de red: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                tvResultsCount.text = "Error de conexión"
            }
        })
    }

    private fun configureAutoComplete() {
        class CaseInsensitiveString(val original: String) {
            override fun toString(): String = original
            override fun equals(other: Any?): Boolean {
                return (other as? CaseInsensitiveString)?.original.equals(original, ignoreCase = true)
            }
            override fun hashCode(): Int = original.lowercase().hashCode()
        }

        lifecycleScope.launch {
            val ubicacionesList = withContext(Dispatchers.Default) {
                ubicacionesDisponibles.map { CaseInsensitiveString(it) }
                    .sortedBy { it.original }
                    .toMutableList()
            }

            withContext(Dispatchers.Main) {
                val autoCompleteAdapter = object : ArrayAdapter<CaseInsensitiveString>(
                    this@ReservarHotel,
                    android.R.layout.simple_dropdown_item_1line,
                    ubicacionesList
                ) {
                    override fun getFilter(): Filter {
                        return object : Filter() {
                            override fun performFiltering(constraint: CharSequence?): FilterResults {
                                val results = FilterResults()
                                if (constraint.isNullOrEmpty()) {
                                    results.values = ubicacionesList
                                    results.count = ubicacionesList.size
                                } else {
                                    val filterPattern = constraint.toString().lowercase().trim()
                                    val filtered = ubicacionesList.filter {
                                        it.original.lowercase().contains(filterPattern)
                                    }
                                    results.values = filtered
                                    results.count = filtered.size
                                }
                                return results
                            }

                            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                                if (results != null && results.count > 0) {
                                    clear()
                                    addAll(results.values as List<CaseInsensitiveString>)
                                    notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }

                etDestino.setAdapter(autoCompleteAdapter)
                etDestino.threshold = 1
                etDestino.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                    val ubicacionSeleccionada = autoCompleteAdapter.getItem(position)?.original ?: ""
                    buscarHotelesPorDestino(ubicacionSeleccionada)
                }
            }
        }
    }

    private fun filtrarYMostrarHoteles(texto: String) {
        if (texto.isNotEmpty()) {
            etDestino.showDropDown()
        }

        // Usar corrutinas para el filtrado pesado
        lifecycleScope.launch {
            val hotelesFiltrados = withContext(Dispatchers.Default) {
                val textoLowerCase = texto.lowercase()
                allHoteles.filter {
                    it.ubicacion.lowercase().contains(textoLowerCase) ||
                            it.nombre.lowercase().contains(textoLowerCase)
                }
            }

            // Actualizar UI en el hilo principal
            withContext(Dispatchers.Main) {
                adapter.updateData(hotelesFiltrados)
                tvResultsCount.text = "Se encontraron ${hotelesFiltrados.size} hoteles"
            }
        }
    }

    private fun buscarHotelesPorDestino(destino: String) {
        if (allHoteles.isEmpty()) {
            Toast.makeText(this, "Esperando a que se carguen los hoteles...", Toast.LENGTH_SHORT)
                .show()
            return
        }

        progressBar.visibility = View.VISIBLE

        // Usar corrutinas para la búsqueda
        lifecycleScope.launch {
            val resultados = withContext(Dispatchers.Default) {
                val destinoLowerCase = destino.lowercase()
                allHoteles.filter {
                    it.ubicacion.lowercase().contains(destinoLowerCase) ||
                            it.nombre.lowercase().contains(destinoLowerCase)
                }
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                adapter.updateData(resultados)
                tvResultsCount.text = "Se encontraron ${resultados.size} hoteles para \"$destino\""

                if (resultados.isEmpty()) {
                    Toast.makeText(this@ReservarHotel, "No hay hoteles disponibles para \"$destino\"", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}