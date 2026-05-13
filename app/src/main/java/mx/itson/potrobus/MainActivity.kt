package mx.itson.potrobus

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        findViewById<MaterialButton>(R.id.btnIniciarSesion).setOnClickListener {
            startActivity(Intent(this, loginActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnRegistrarse).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}