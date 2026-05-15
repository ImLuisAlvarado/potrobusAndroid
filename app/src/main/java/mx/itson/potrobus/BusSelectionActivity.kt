package mx.itson.potrobus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import mx.itson.potrobus.adapters.BusAdapter
import mx.itson.potrobus.entities.Unidad
import mx.itson.potrobus.utils.RetrofitUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class BusSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_selection)

        val token = getSharedPreferences("potrobus_prefs", MODE_PRIVATE)
            .getString("jwt_token", "") ?: ""

        Log.d("BUS_SELECTION", "Token: $token")
        Log.d("BUS_SELECTION", "Llamando a getBusesActivos...")

        val recycler = findViewById<RecyclerView>(R.id.rvBuses)
        recycler.layoutManager = LinearLayoutManager(this)

        val progress = findViewById<android.widget.ProgressBar>(R.id.progressBuses)
        progress.visibility = View.VISIBLE

        if (!isOnline()) {
            progress.visibility = View.GONE
            Toast.makeText(this, "Sin conexión a internet — verifica tu red", Toast.LENGTH_LONG).show()
            recycler.postDelayed({ recreate() }, 5000)
            return
        }

        RetrofitUtil.getApiPotrobusAPI().getBusesActivos("Bearer $token")
            .enqueue(object : Callback<List<Unidad>> {
                override fun onResponse(call: Call<List<Unidad>>, response: Response<List<Unidad>>) {
                    progress.visibility = View.GONE
                    Log.d("BUS_SELECTION", "CODE: ${response.code()}")
                    Log.d("BUS_SELECTION", "RAW BODY: ${response.body()}")
                    Log.d("BUS_SELECTION", "Respuesta: ${response.code()} - ${response.body()}")
                    val buses = response.body() ?: emptyList()
                    if (buses.isEmpty()) {
                        runOnUiThread {
                            Toast.makeText(this@BusSelectionActivity,
                                "No hay autobuses activos", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }
                    runOnUiThread {
                        recycler.adapter = BusAdapter(buses) { bus ->
                            val intent =
                                Intent(this@BusSelectionActivity, MapViewActivity::class.java)
                            intent.putExtra("id_unidad", bus.id_unidad ?: 1)
                            intent.putExtra("numero_economico", bus.numero_economico ?: "PotroBus")
                            startActivity(intent)
                        }
                    }
                }

                override fun onFailure(call: Call<List<Unidad>>, t: Throwable) {
                    Log.e("BUS_SELECTION", "Error: ${t.message}")
                    runOnUiThread {
                        progress.visibility = View.GONE
                        val msg = if (!isOnline()) "Sin conexión a internet" else "Error al conectar con el servidor"
                        Toast.makeText(this@BusSelectionActivity, msg, Toast.LENGTH_LONG).show()
                        recycler.postDelayed({ recreate() }, 3000)
                    }
                }
            })
    }


    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


}