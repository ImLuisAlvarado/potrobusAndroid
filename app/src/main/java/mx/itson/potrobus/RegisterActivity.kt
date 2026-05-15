package mx.itson.potrobus

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import mx.itson.potrobus.entities.RegisterRequest
import mx.itson.potrobus.entities.RegisterResponse
import mx.itson.potrobus.utils.RetrofitUtil
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private val DOMINIOS_ITSON = listOf("@itson.edu.mx", "@potros.itson.edu.mx")

    private lateinit var tilNombre: TextInputLayout
    private lateinit var tilApellido: TextInputLayout
    private lateinit var tilCorreo: TextInputLayout
    private lateinit var tilPassword: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        tilNombre   = findViewById(R.id.tilNombre)
        tilApellido = findViewById(R.id.tilApellido)
        tilCorreo   = findViewById(R.id.tilCorreo)
        tilPassword = findViewById(R.id.tilPassword)

        val etNombre     = findViewById<TextInputEditText>(R.id.etNombre)
        val etApellido   = findViewById<TextInputEditText>(R.id.etApellido)
        val etCorreo     = findViewById<TextInputEditText>(R.id.etCorreo)
        val etPassword   = findViewById<TextInputEditText>(R.id.etPassword)
        val btnRegistrar = findViewById<MaterialButton>(R.id.btnRegistrar)

        etNombre.setOnFocusChangeListener   { _, _ -> tilNombre.error = null }
        etApellido.setOnFocusChangeListener { _, _ -> tilApellido.error = null }
        etCorreo.setOnFocusChangeListener   { _, _ -> tilCorreo.error = null }
        etPassword.setOnFocusChangeListener { _, _ -> tilPassword.error = null }

        btnRegistrar.setOnClickListener {
            val nombre   = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val correo   = etCorreo.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()

            tilNombre.error   = null
            tilApellido.error = null
            tilCorreo.error   = null
            tilPassword.error = null

            if (!validarCampos(nombre, apellido, correo, password)) return@setOnClickListener

            btnRegistrar.isEnabled = false
            btnRegistrar.text = "Registrando..."

            RetrofitUtil.getApiPotrobusAPI()
                .register(RegisterRequest(nombre, apellido, correo, password))
                .enqueue(object : Callback<RegisterResponse> {
                    override fun onResponse(call: Call<RegisterResponse>,
                                            response: Response<RegisterResponse>) {
                        btnRegistrar.isEnabled = true
                        btnRegistrar.text = "Registrarse"

                        if (response.isSuccessful) {
                            Toast.makeText(this@RegisterActivity,
                                "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterActivity, loginActivity::class.java))
                            finish()
                        } else {
                            val msg = try {
                                JSONObject(response.errorBody()?.string() ?: "")
                                    .getString("error")
                            } catch (e: Exception) {
                                "Error al registrar"
                            }
                            // Correo duplicado → error en el campo
                            if (response.code() == 409) {
                                tilCorreo.error = msg
                            } else {
                                Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                        btnRegistrar.isEnabled = true
                        btnRegistrar.text = "Registrarse"
                        Toast.makeText(this@RegisterActivity,
                            "Sin conexión al servidor", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun validarCampos(
        nombre: String, apellido: String, correo: String, password: String
    ): Boolean {
        var esValido = true

        if (nombre.isEmpty()) {
            tilNombre.error = "El nombre es obligatorio"
            esValido = false
        } else if (nombre.length < 2) {
            tilNombre.error = "Nombre demasiado corto"
            esValido = false
        }

        if (apellido.isEmpty()) {
            tilApellido.error = "El apellido es obligatorio"
            esValido = false
        } else if (apellido.length < 2) {
            tilApellido.error = "Apellido demasiado corto"
            esValido = false
        }

        if (correo.isEmpty()) {
            tilCorreo.error = "El correo es obligatorio"
            esValido = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            tilCorreo.error = "Ingresa un correo válido (ej: nombre@itson.edu.mx)"
            esValido = false
        } else if (DOMINIOS_ITSON.none { correo.endsWith(it) }) {
            tilCorreo.error = "Usa tu correo institucional (@itson.edu.mx o @potros.itson.edu.mx)"
            esValido = false
        }

        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es obligatoria"
            esValido = false
        } else if (password.length < 8) {
            tilPassword.error = "Mínimo 8 caracteres"
            esValido = false
        }

        return esValido
    }
}