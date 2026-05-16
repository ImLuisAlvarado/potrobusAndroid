package mx.itson.potrobus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mx.itson.potrobus.adapters.BusAdapter
import mx.itson.potrobus.entities.Unidad
import mx.itson.potrobus.utils.RetrofitUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class BusSelectionActivity : AppCompatActivity() {

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshRunnable = Runnable { cargarBuses() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_selection)
        findViewById<RecyclerView>(R.id.rvBuses).layoutManager = LinearLayoutManager(this)
        cargarBuses()
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(refreshRunnable, 30_000)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun cargarBuses() {
        val token = getSharedPreferences("potrobus_prefs", MODE_PRIVATE)
            .getString("jwt_token", "") ?: ""

        val recycler = findViewById<RecyclerView>(R.id.rvBuses)
        val progress = findViewById<android.widget.ProgressBar>(R.id.progressBuses)

        progress.visibility = View.VISIBLE

        if (!isOnline()) {
            progress.visibility = View.GONE
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            handler.postDelayed(refreshRunnable, 30_000)
            return
        }

        RetrofitUtil.getApiPotrobusAPI().getBusesActivos("Bearer $token")
            .enqueue(object : Callback<List<Unidad>> {
                override fun onResponse(call: Call<List<Unidad>>, response: Response<List<Unidad>>) {
                    progress.visibility = View.GONE
                    Log.d("BUS_SELECTION", "CODE: ${response.code()} BODY: ${response.body()}")

                    val buses = response.body() ?: emptyList()
                    runOnUiThread {
                        if (buses.isEmpty()) {
                            Toast.makeText(this@BusSelectionActivity,
                                "No hay autobuses activos", Toast.LENGTH_SHORT).show()
                        } else {
                            recycler.adapter = BusAdapter(buses) { bus ->
                                val intent = Intent(this@BusSelectionActivity, MapViewActivity::class.java)
                                intent.putExtra("id_unidad", bus.id_unidad ?: 1)
                                intent.putExtra("numero_economico", bus.numero_economico ?: "PotroBus")
                                startActivity(intent)
                            }
                        }
                    }
                    handler.postDelayed(refreshRunnable, 30_000)
                }

                override fun onFailure(call: Call<List<Unidad>>, t: Throwable) {
                    progress.visibility = View.GONE
                    Log.e("BUS_SELECTION", "Error: ${t.message}")
                    runOnUiThread {
                        val msg = if (!isOnline()) "Sin conexión a internet" else "Error al conectar con el servidor"
                        Toast.makeText(this@BusSelectionActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                    handler.postDelayed(refreshRunnable, 30_000)
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