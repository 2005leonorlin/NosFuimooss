package com.example.nosfuimooss.hotelyboleto


import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.NosFuimooss.R
import com.example.nosfuimooss.Adapter.ElegirVuelosAdapter
import com.example.nosfuimooss.api.RetrofitClient
import com.example.nosfuimooss.boleto.SeleccionarBoletoIdaActivity
import com.example.nosfuimooss.model.Boleto
import com.example.nosfuimooss.model.Vuelo
import com.example.nosfuimooss.usuario.PreferenciasUsuario
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class ElegirFragmentVuelosActivity : AppCompatActivity() {
    private lateinit var spinnerOrigen: Spinner
    private lateinit var spinnerDestino: Spinner
    private lateinit var btnFechaInicio: Button
    private lateinit var btnFechaFin: Button
    private lateinit var btnMenos: Button
    private lateinit var btnMas: Button
    private lateinit var txtCantidadPersonas: TextView
    private lateinit var spinnerCategoria: Spinner
    private lateinit var radioGroupTipoVuelo: RadioGroup
    private lateinit var rbIda: RadioButton
    private lateinit var rbIdaVuelta: RadioButton
    private lateinit var txtPrecioTotal: TextView
    private lateinit var btnBuscar: Button
    private lateinit var recyclerVuelos: RecyclerView
    private lateinit var btnSeleccionarFechas: Button
    private var allVuelos: List<Vuelo> = emptyList()
    private var destinoPreseleccionado: String? = null
    private var origenesList = mutableListOf<String>()
    private var destinosList = mutableListOf<String>()
    private var fechaInicio: Date? = null
    private var fechaFin: Date? = null
    private var cantidadPersonas = 1
    private val categorias = listOf("Turista", "Turista Premium", "Business", "Primera Clase")
    private var precioBase = 0.0
    private var precioTotal = 0.0

    private lateinit var adapter: ElegirVuelosAdapter
    private var allBoletos: List<Boleto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_elegir_fragment_vuelos)

        destinoPreseleccionado = intent.getStringExtra("destino")

        initViews()
        setupSpinners()
        setupListeners()
        fetchOrigenesYDestinos()
        fetchVuelos()
    }

    private fun initViews() {
        spinnerOrigen = findViewById(R.id.spinnerOrigen)
        spinnerDestino = findViewById(R.id.spinnerDestino)
        btnSeleccionarFechas = findViewById(R.id.btnSeleccionarFechas)
        btnFechaInicio = findViewById(R.id.btnFechaInicio)
        btnFechaFin = findViewById(R.id.btnFechaFin)
        btnMenos = findViewById(R.id.btnMenos)
        btnMas = findViewById(R.id.btnMas)
        txtCantidadPersonas = findViewById(R.id.txtCantidadPersonas)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        radioGroupTipoVuelo = findViewById(R.id.radioGroupTipoVuelo)
        rbIda = findViewById(R.id.rbIda)
        rbIdaVuelta = findViewById(R.id.rbIdaVuelta)
        txtPrecioTotal = findViewById(R.id.txtPrecioTotal)
        btnBuscar = findViewById(R.id.btnBuscar)
        recyclerVuelos = findViewById(R.id.recyclerVuelos)

        recyclerVuelos.layoutManager = LinearLayoutManager(this)
        adapter = ElegirVuelosAdapter(allBoletos, allVuelos,"") { boleto ->
            // 1) Buscamos el Vuelo que corresponde al destino del boleto
            val vueloCoincidente = allVuelos
                .find { it.nombre.equals(boleto.destino, ignoreCase = true) }

            // 2) Sacamos la URL principal (o el primer elemento de imágenes)
            val imageUrl = vueloCoincidente?.principal
                ?: vueloCoincidente?.imagenes?.firstOrNull()
                ?: ""

            // 3) Preferencias de usuario
            val prefs = PreferenciasUsuario(
                spinnerOrigen.selectedItem.toString(),
                spinnerDestino.selectedItem.toString(),
                fechaInicio?.time ?: 0L,
                fechaFin?.time ?: 0L,
                cantidadPersonas,
                spinnerCategoria.selectedItem.toString()
            )

            // 4) Montamos el Intent incluyendo imageUrl
            Intent(
                this@ElegirFragmentVuelosActivity,
                SeleccionarBoletoIdaActivity::class.java
            ).apply {
                putExtra("boleto", boleto)
                putExtra("preferencias", prefs)
                putExtra("precioTotal", precioTotal)
                putExtra("isRoundTrip", rbIdaVuelta.isChecked)
                putExtra("imageUrl", imageUrl)
            }.also { startActivity(it) }
        }
        recyclerVuelos.adapter = adapter
    }

    private fun setupSpinners() {
        val categoriaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        categoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = categoriaAdapter
    }

    private fun setupListeners() {
        btnFechaInicio.setOnClickListener { showDatePickerDialog(true) }
        btnFechaFin.setOnClickListener { showDatePickerDialog(false) }

        btnMenos.setOnClickListener {
            if (cantidadPersonas > 1) {
                cantidadPersonas--
                txtCantidadPersonas.text = cantidadPersonas.toString()
                calcularPrecioTotal()
            }
        }

        btnMas.setOnClickListener {
            cantidadPersonas++
            txtCantidadPersonas.text = cantidadPersonas.toString()
            calcularPrecioTotal()
        }

        btnSeleccionarFechas.setOnClickListener {
            btnFechaInicio.visibility = View.VISIBLE
            btnFechaFin.visibility = View.VISIBLE
            btnSeleccionarFechas.visibility = View.GONE
        }

        spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                calcularPrecioTotal()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Listener para el spinner de origen - filtrar automáticamente
        spinnerOrigen.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                filtrarBoletosAutomaticamente()
                calcularPrecioTotal()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Listener para el spinner de destino - filtrar automáticamente
        spinnerDestino.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                filtrarBoletosAutomaticamente()
                calcularPrecioTotal()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        radioGroupTipoVuelo.setOnCheckedChangeListener { _, _ -> calcularPrecioTotal() }
        btnBuscar.setOnClickListener { buscarVuelos() }
    }

    private fun datosCompletos(): Boolean {
        val origenSeleccionado = spinnerOrigen.selectedItem as? String
        val destinoSeleccionado = spinnerDestino.selectedItem as? String
        val categoriaSeleccionada = spinnerCategoria.selectedItemPosition
        val tipoVueloSeleccionado = radioGroupTipoVuelo.checkedRadioButtonId != -1

        return !origenSeleccionado.isNullOrEmpty() &&
                !destinoSeleccionado.isNullOrEmpty() &&
                fechaInicio != null &&
                cantidadPersonas > 0 &&
                categoriaSeleccionada >= 0 &&
                tipoVueloSeleccionado
    }

    private fun fetchOrigenesYDestinos() {
        RetrofitClient.boletoApiService.getAllBoletos().enqueue(object : Callback<List<Boleto>> {
            override fun onResponse(call: Call<List<Boleto>>, response: Response<List<Boleto>>) {
                if (response.isSuccessful) {
                    allBoletos = response.body() ?: emptyList()

                    origenesList.clear()
                    destinosList.clear()

                    for (boleto in allBoletos) {
                        if (!origenesList.contains(boleto.origen)) origenesList.add(boleto.origen)
                        if (!destinosList.contains(boleto.destino)) destinosList.add(boleto.destino)
                    }

                    origenesList.sort()
                    destinosList.sort()

                    setupOrigenesSpinner()
                    setupDestinosSpinner()

                    // Si hay destino preseleccionado, seleccionarlo en el spinner
                    if (!destinoPreseleccionado.isNullOrEmpty()) {
                        val index = destinosList.indexOf(destinoPreseleccionado)
                        if (index >= 0) {
                            spinnerDestino.setSelection(index)
                        }
                    }

                    // Filtrar automáticamente después de configurar los spinners
                    // Usar un pequeño delay para asegurar que las selecciones se han aplicado
                    recyclerVuelos.post {
                        filtrarBoletosAutomaticamente()
                    }

                    if (allBoletos.isNotEmpty()) {
                        precioBase = allBoletos[0].precio
                        calcularPrecioTotal()
                    }
                } else {
                    Toast.makeText(
                        this@ElegirFragmentVuelosActivity,
                        "Error al cargar datos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<Boleto>>, t: Throwable) {
                Toast.makeText(
                    this@ElegirFragmentVuelosActivity,
                    "Error de conexión",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ElegirFragmentVuelosActivity", "Error: ${t.message}", t)
            }
        })
    }

    private fun setupOrigenesSpinner() {
        val origenAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, origenesList)
        origenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOrigen.adapter = origenAdapter

        // Establecer Alicante como predeterminado para origen
        val alicanteIndex = origenesList.indexOfFirst { it.equals("Alicante", ignoreCase = true) }
        if (alicanteIndex >= 0) {
            spinnerOrigen.setSelection(alicanteIndex)
        }
    }

    private fun setupDestinosSpinner() {
        val destinoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, destinosList)
        destinoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDestino.adapter = destinoAdapter

        // Establecer Atenas como predeterminado para destino (si no hay destino preseleccionado)
        if (destinoPreseleccionado.isNullOrEmpty()) {
            val atenasIndex = destinosList.indexOfFirst { it.equals("Atenas", ignoreCase = true) }
            if (atenasIndex >= 0) {
                spinnerDestino.setSelection(atenasIndex)
            }
        }
    }

    // Método para filtrar boletos automáticamente cuando cambian los spinners
    private fun filtrarBoletosAutomaticamente() {
        val origenSeleccionado = spinnerOrigen.selectedItem as? String
        val destinoSeleccionado = spinnerDestino.selectedItem as? String

        if (origenSeleccionado.isNullOrEmpty() || destinoSeleccionado.isNullOrEmpty()) {
            // Si no hay selección válida, mostrar todos los boletos
            adapter.updateData(allBoletos)
            return
        }

        val resultados = allBoletos.filter {
            it.origen.equals(origenSeleccionado, ignoreCase = true) &&
                    it.destino.equals(destinoSeleccionado, ignoreCase = true)
        }

        adapter.updateData(resultados)

        // Opcional: mostrar mensaje informativo
        if (resultados.isEmpty()) {
            Log.d(
                "ElegirFragmentVuelosActivity",
                "No hay boletos para $origenSeleccionado -> $destinoSeleccionado"
            )
        } else {
            Log.d(
                "ElegirFragmentVuelosActivity",
                "Se encontraron ${resultados.size} boletos para $origenSeleccionado -> $destinoSeleccionado"
            )
        }
    }

    // Método alternativo para buscar solo por destino (mantenido por compatibilidad)
    private fun buscarBoletosPorDestino(destino: String) {
        val resultados = allBoletos.filter { it.destino.contains(destino, ignoreCase = true) }
        adapter.updateData(resultados)

        if (resultados.isEmpty()) {
            Toast.makeText(this, "No hay boletos disponibles para $destino", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun showDatePickerDialog(isInicio: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog =
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val selectedDate = calendar.time
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate)

                if (isInicio) {
                    fechaInicio = selectedDate
                    btnFechaInicio.text = formattedDate

                    if ((fechaFin != null) && (fechaFin!! < fechaInicio!!)) {
                        fechaFin = selectedDate
                        btnFechaFin.text = formattedDate
                    }
                } else {
                    if (fechaInicio != null && selectedDate < fechaInicio!!) {
                        Toast.makeText(
                            this,
                            "La fecha de fin debe ser posterior a la de inicio",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@DatePickerDialog
                    }

                    fechaFin = selectedDate
                    btnFechaFin.text = formattedDate
                }

                calcularPrecioTotal()
            }, year, month, day)

        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    private fun calcularPrecioTotal() {
        val origenSeleccionado = spinnerOrigen.selectedItem as? String ?: return
        val destinoSeleccionado = spinnerDestino.selectedItem as? String ?: return

        // Buscar el precio base del boleto con ese origen y destino
        val boleto = allBoletos.find {
            it.origen.equals(origenSeleccionado, ignoreCase = true) &&
                    it.destino.equals(destinoSeleccionado, ignoreCase = true)
        }

        if (boleto == null) {
            txtPrecioTotal.text = "Destino no disponible"
            return
        }

        val precioBoleto = boleto.precio
        var total = precioBoleto * cantidadPersonas

        // Sumar el precio de la categoría
        total += when (spinnerCategoria.selectedItem.toString()) {
            "Turista" -> 1
            "Turista Premium" -> 10
            "Business" -> 20
            "Primera Clase" -> 50
            else -> 0
        }

        precioTotal = total
        txtPrecioTotal.text = "Precio total: ${precioTotal.toInt()} €"
    }

    private fun fetchVuelos() {
        RetrofitClient.vueloApiService.getAllVuelos().enqueue(object : Callback<List<Vuelo>> {
            override fun onResponse(call: Call<List<Vuelo>>, response: Response<List<Vuelo>>) {
                if (!response.isSuccessful) return
                allVuelos = response.body().orEmpty()
                // Actualiza vuelos del adaptador
                adapter.updateVuelos(allVuelos)
            }

            override fun onFailure(call: Call<List<Vuelo>>, t: Throwable) {
                Log.e("ElegirFragmentVuelosActivity", "Error Vuelos: ${t.message}")
            }
        })
    }

    private fun buscarVuelos() {
        if (!datosCompletos()) {
            Toast.makeText(
                this,
                "Por favor, completa todos los campos antes de buscar",
                Toast.LENGTH_SHORT
            ).show()
            return
        }


        val origenSeleccionado = spinnerOrigen.selectedItem as String
        val destinoSeleccionado = spinnerDestino.selectedItem as String

        val resultados = allBoletos.filter {
            it.origen.equals(origenSeleccionado, ignoreCase = true) &&
                    it.destino.equals(destinoSeleccionado, ignoreCase = true)
        }

        if (resultados.isEmpty()) {
            Toast.makeText(
                this,
                "No hay boletos disponibles para esta búsqueda",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Mostrando ${resultados.size} vuelos encontrados",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
