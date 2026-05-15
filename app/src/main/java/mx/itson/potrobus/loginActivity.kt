package mx.itson.potrobus

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.util.Patterns
import mx.itson.potrobus.entities.LoginRequest
import mx.itson.potrobus.utils.RetrofitUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class loginActivity : AppCompatActivity() {
    private val DOMINIOS_ITSON = listOf("@itson.edu.mx", "@potros.itson.edu.mx")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val token = getSharedPreferences("potrobus_prefs", MODE_PRIVATE)
            .getString("jwt_token", "")
        if (!token.isNullOrEmpty()) {
            startActivity(Intent(this, BusSelectionActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_login)

        val tilUser     = findViewById<TextInputLayout>(R.id.tilUser)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etUser      = findViewById<TextInputEditText>(R.id.etUser)
        val etPassword  = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin    = findViewById<MaterialButton>(R.id.btnLogin)

        etUser.setOnFocusChangeListener     { _, _ -> tilUser.error = null }
        etPassword.setOnFocusChangeListener { _, _ -> tilPassword.error = null }


        btnLogin.setOnClickListener {
            val correo   = etUser.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()

            tilUser.error     = null
            tilPassword.error = null

            // Validaciones
            var esValido = true

            if (correo.isEmpty()) {
                tilUser.error = "El correo es obligatorio"
                esValido = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                tilUser.error = "Ingresa un correo válido"
                esValido = false
            } else if (DOMINIOS_ITSON.none { correo.endsWith(it) }) {
                tilUser.error = "Usa tu correo institucional (@itson.edu.mx)"
                esValido = false
            }

            if (password.isEmpty()) {
                tilPassword.error = "La contraseña es obligatoria"
                esValido = false
            }

            if (!esValido) return@setOnClickListener

            btnLogin.isEnabled = false
            btnLogin.text = "Iniciando..."
            RetrofitUtil.getApiPotrobusAPI().login(LoginRequest(correo, password))
                .enqueue(object : Callback<mx.itson.potrobus.entities.LoginResponse> {
                    override fun onResponse(call: Call<mx.itson.potrobus.entities.LoginResponse>,
                                            response: Response<mx.itson.potrobus.entities.LoginResponse>) {
                        btnLogin.isEnabled = true
                        btnLogin.text = "INICIAR SESIÓN"

                        if (response.isSuccessful) {
                            val body = response.body()!!
                            getSharedPreferences("potrobus_prefs", MODE_PRIVATE).edit().apply {
                                putString("jwt_token", body.access_token)
                                putString("user_nombre", body.user.nombre)
                                putString("user_correo", body.user.correo)
                                putString("user_rol",    body.user.rol)
                                apply()
                            }
                            startActivity(Intent(this@loginActivity, BusSelectionActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@loginActivity,
                                "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<mx.itson.potrobus.entities.LoginResponse>, t: Throwable) {
                        btnLogin.isEnabled = true
                        btnLogin.text = "INICIAR SESIÓN"
                        Toast.makeText(this@loginActivity,
                            "Sin conexión al servidor", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
        }
