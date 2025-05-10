package com.example.nosfuimooss.boleto

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.NosFuimooss.R

class DetalleBoletosActivity : AppCompatActivity() {
    private lateinit var txtRuta: TextView
    private lateinit var txtFechas: TextView
    private lateinit var txtPrecio: TextView
    private lateinit var txtDetalles: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_boletos)
        supportActionBar?.hide()

    }
}