package com.example.nosfuimooss.sesion

import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import com.example.NosFuimooss.databinding.ActivityRegisterBinding
import com.example.nosfuimooss.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.registerButton.setOnClickListener {
            val name = binding.nameInput.text.toString()
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()

            if (validateInput(name, email, password, confirmPassword)) {
                registerUser(email, password, name, apellido = "") // Apellido no se usa en este caso
            }
        }
    }

    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        if (TextUtils.isEmpty(name)) {
            showError("El campo Nombre y Apellido no puede estar vacío")
            return false
        }

        if (TextUtils.isEmpty(email)) {
            showError("El campo Correo Electrónico no puede estar vacío")
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("El correo electrónico no tiene un formato válido")
            return false
        }

        if (TextUtils.isEmpty(password)) {
            showError("El campo Contraseña no puede estar vacío")
            return false
        }

        if (password.length < 8 || !password.matches(Regex(".*\\d.*")) || !password.matches(Regex(".*[A-Z].*"))) {
            showError("La contraseña debe tener al menos 8 caracteres, una mayúscula y un número")
            return false
        }

        if (password != confirmPassword) {
            showError("Las contraseñas no coinciden")
            return false
        }

        return true
    }

    private fun registerUser(email: String, password: String, name: String, apellido: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser

                    val userId = user?.uid
                    val database = FirebaseDatabase.getInstance().reference
                    val userData = User(name, apellido, email)

                    if (userId != null) {
                        database.child("users").child(userId).setValue(userData)
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                    finish()
                                } else {
                                    showError("Error al guardar los datos adicionales")
                                }
                            }
                    }
                } else {
                    showError("Error al registrar el usuario: ${task.exception?.message}")
                }
            }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}