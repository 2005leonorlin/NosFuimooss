package com.example.nosfuimooss.nologeado


import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nosfuimooss.sesion.LoginActivity
import com.example.NosFuimooss.R
import com.example.nosfuimooss.Adapter.CategoriaAdapter
import com.example.nosfuimooss.Adapter.DestinoAdapter
import com.example.nosfuimooss.api.RetrofitClient
import com.example.nosfuimooss.model.Categoria
import com.example.nosfuimooss.model.Vuelo





class UsuarioNoLogeadoInicioActivity : AppCompatActivity() {

    private lateinit var categoriaAdapter: CategoriaAdapter
    private lateinit var destinoAdapter: DestinoAdapter
    private lateinit var recyclerCategorias: RecyclerView
    private lateinit var recyclerDestinos: RecyclerView
    private val favoritosIds: List<String> = emptyList()
    private var destinosActuales: List<Vuelo> = emptyList()


    // Lista fija de categorías
    private val categoriasFijas = listOf(
        Categoria("Popular"),
        Categoria("Playa"),
        Categoria("Naturaleza"),
        Categoria("Urbano"),
        Categoria("Cultural"),
        Categoria("Exótico")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_usuario_no_logeado_inicio)

        recyclerCategorias = findViewById(R.id.recycler_categorias)
        recyclerDestinos = findViewById(R.id.recycler_destinos)

        recyclerCategorias.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerDestinos.layoutManager = GridLayoutManager(this, 2)
        categoriaAdapter = CategoriaAdapter(categoriasFijas) { categoria ->
            fetchDestinosFiltrados(categoria.nombre)
        }
        recyclerCategorias.adapter = categoriaAdapter
        destinoAdapter = DestinoAdapter(emptyList(), favoritosIds)
        recyclerDestinos.adapter = destinoAdapter
        fetchDestinosFiltrados(categoriasFijas[0].nombre)
        val irALogin = Intent(this, LoginActivity::class.java)
        findViewById<ImageView>(R.id.ic_user).setOnClickListener { startActivity(irALogin) }
        findViewById<ImageView>(R.id.ic_calendar).setOnClickListener { startActivity(irALogin) }
        findViewById<ImageView>(R.id.nav_home).setOnClickListener { startActivity(irALogin) }
        findViewById<ImageView>(R.id.nav_flight).setOnClickListener { startActivity(irALogin) }
        findViewById<ImageView>(R.id.nav_moon).setOnClickListener { startActivity(irALogin) }
        findViewById<ImageView>(R.id.nav_heart).setOnClickListener { startActivity(irALogin) }
        findViewById<ImageView>(R.id.nav_profile).setOnClickListener { startActivity(irALogin) }
        findViewById<TextView>(R.id.btn_iniciar_sesion).setOnClickListener { startActivity(irALogin) }
    }


    private fun fetchDestinosFiltrados(nombreCategoria: String) {
        val call = RetrofitClient.vueloApiService.getVuelosByCategoria(nombreCategoria)

        call.enqueue(object : retrofit2.Callback<List<Vuelo>> {
            override fun onResponse(
                call: retrofit2.Call<List<Vuelo>>,
                response: retrofit2.Response<List<Vuelo>>
            ) {
                if (response.isSuccessful) {
                    destinosActuales = response.body() ?: emptyList()
                    destinoAdapter.updateFavoritos(destinosActuales, favoritosIds)
                } else {
                    Toast.makeText(this@UsuarioNoLogeadoInicioActivity, "Error en respuesta de API", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<List<Vuelo>>, t: Throwable) {
                Toast.makeText(this@UsuarioNoLogeadoInicioActivity, "Error al conectar con API", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
