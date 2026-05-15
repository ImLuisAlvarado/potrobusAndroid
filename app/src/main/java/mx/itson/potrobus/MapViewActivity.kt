package mx.itson.potrobus

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolylineOptions
import mx.itson.potrobus.entities.Parada
import com.google.android.gms.maps.model.Polyline
import mx.itson.potrobus.adapters.ParadasAdapter
import mx.itson.potrobus.utils.Constants
import android.net.ConnectivityManager
import android.net.NetworkCapabilities


class MapViewActivity : AppCompatActivity(), OnMapReadyCallback {
    private var map: GoogleMap? = null
    private var socket: Socket? = null
    private var busMarker: Marker? = null
    private val paradaCoords = mutableListOf<LatLng>()
    private var rutaPendientePolyline: Polyline? = null
    private val rutaCompleta = mutableListOf<LatLng>()
    private var indiceParadaActual = -1
    private val BASE_URL = Constants.BASE_URL.dropLast(1)
    private val GUAYMAS_CENTER = LatLng(27.9600, -110.8600) // coordenada inicial
    private var idUnidadSeleccionada = 1
    private var lastGpsTime = 0L
    private val GPS_TIMEOUT_MS = 30_000L // 30 segundos sin señal = perdida

    private var signalLost = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_view)

        idUnidadSeleccionada = intent.getIntExtra("id_unidad", 1)
        val numeroEconomico = intent.getStringExtra("numero_economico") ?: "PotroBus"
        findViewById<TextView>(R.id.tvParadasTitle).text = "Ruta — $numeroEconomico"

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (!isOnline()) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        connectSocket()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map?.apply {
            moveCamera(CameraUpdateFactory.newLatLngZoom(GUAYMAS_CENTER, 14f))
            uiSettings.isZoomControlsEnabled = true
        }

        val token = getSharedPreferences("potrobus_prefs", MODE_PRIVATE)
            .getString("jwt_token", "") ?: ""

        Parada().getByRuta(token, idUnidadSeleccionada) { paradas ->
            if (paradas.isNullOrEmpty()) return@getByRuta
            runOnUiThread {
                // Marcadores de paradas
                paradas.forEach { parada ->
                    val lat = parada.latitud ?: return@forEach
                    val lng = parada.longitud ?: return@forEach
                    val pos = LatLng(lat, lng)
                    paradaCoords.add(pos)
                    map?.addMarker(
                        MarkerOptions()
                            .position(pos)
                            .title(parada.nombre)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )
                }

                rutaCompleta.clear()
                rutaCompleta.addAll(paradaCoords)

                rutaPendientePolyline?.remove()
                rutaPendientePolyline = map?.addPolyline(
                    PolylineOptions()
                        .addAll(rutaCompleta)
                        .color(android.graphics.Color.parseColor("#1565C0"))
                        .width(8f)
                        .geodesic(true)
                )
                setupParadasList(paradas)
            }
        }
    }

    private fun setupParadasList(paradas: List<Parada>) {
        val recycler = findViewById<RecyclerView>(R.id.rvParadas)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = ParadasAdapter(paradas)
    }


    private fun connectSocket() {
        val token = getSharedPreferences("potrobus_prefs", MODE_PRIVATE)
            .getString("jwt_token", "") ?: ""

        if (token.isEmpty()) {
            getSharedPreferences("potrobus_prefs", MODE_PRIVATE).edit()
                .remove("jwt_token").apply()
            Toast.makeText(this, "Sesión expirada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            val options = IO.Options.builder()
                .setQuery("token=$token&id_unidad=$idUnidadSeleccionada")
                .setReconnection(true)
                .build()

            socket = IO.socket(BASE_URL, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("Socket", "Conectado")
                runOnUiThread {
                    Toast.makeText(this, "Conectado al servidor", Toast.LENGTH_SHORT).show()
                }
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args.firstOrNull()?.toString() ?: ""
                Log.e("Socket", "Error: $error")
                if (error.contains("401") || error.contains("expired") || error.contains("Token")) {
                    runOnUiThread {
                        getSharedPreferences("potrobus_prefs", MODE_PRIVATE).edit()
                            .remove("jwt_token").apply()
                        Toast.makeText(this@MapViewActivity, "Sesión expirada, inicia sesión de nuevo", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }

            socket?.on("gps_live") { args ->
                try {
                    lastGpsTime = System.currentTimeMillis()
                    runOnUiThread {
                        if (signalLost) {
                            signalLost = false
                            busMarker?.title = "PotroBus"
                            busMarker?.hideInfoWindow()
                            Toast.makeText(this@MapViewActivity, "Señal GPS recuperada", Toast.LENGTH_SHORT).show()
                        }
                    }
                    val data = args[0] as JSONObject

                    val lat = data.getDouble("lat")
                    val lng = data.getDouble("lng")
                    runOnUiThread { updateBusMarker(LatLng(lat, lng)) }
                } catch (e: Exception) {
                    Log.e("Socket", "Error parseando gps_live: ${e.message}")
                }
            }

            socket?.on("notificacion") { args ->
                try {
                    val data = args[0] as JSONObject
                    val tipo = data.optString("tipo", "INFO")
                    val mensaje = data.optString("mensaje", "")
                    runOnUiThread {
                        Toast.makeText(this, "[$tipo] $mensaje", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("Socket", "Error en notificación: ${e.message}")
                }
            }

            socket?.connect()
            val handler = android.os.Handler(mainLooper)
            val checkGps = object : Runnable {
                override fun run() {
                    if (lastGpsTime > 0 && System.currentTimeMillis() - lastGpsTime > GPS_TIMEOUT_MS) {
                        if (!signalLost) {
                            signalLost = true
                            busMarker?.title = "Señal perdida"
                            busMarker?.showInfoWindow()
                            Toast.makeText(this@MapViewActivity, "Señal GPS perdida", Toast.LENGTH_SHORT).show()
                        }
                    }
                    handler.postDelayed(this, 5000)
                }
            }
            handler.post(checkGps)

        } catch (e: Exception) {
            Log.e("Socket", "Error iniciando socket: ${e.message}")
        }
    }

    private fun updateBusMarker(pos: LatLng) {
        if (busMarker == null) {
            val density = resources.displayMetrics.density
            val sizePx = (48 * density).toInt()
            val busIcon = BitmapDescriptorFactory.fromBitmap(
                android.graphics.Bitmap.createScaledBitmap(
                    android.graphics.BitmapFactory.decodeResource(resources, R.drawable.ic_bus),
                    sizePx, sizePx, false
                )
            )
            busMarker = map?.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("PotroBus")
                    .icon(busIcon)
            )
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f))
        } else {
            busMarker?.position = pos
        }

        if (rutaCompleta.isEmpty()) return

        // encontrar punto más cercano en la ruta
        val indiceMasCercano = rutaCompleta.indices.minByOrNull { i ->
            distancia(pos, rutaCompleta[i])
        } ?: return

        // solo recortar si el bus está cerca de la ruta (menos de 500 metros)
        val distanciaAlPunto =
            kotlin.math.sqrt(distancia(pos, rutaCompleta[indiceMasCercano])) * 111000
        if (distanciaAlPunto > 500) return  // está muy lejos, no tocar la línea


        val rutaPendiente = rutaCompleta.subList(indiceMasCercano, rutaCompleta.size)

        rutaPendientePolyline?.remove()
        if (rutaPendiente.size >= 2) {
            rutaPendientePolyline = map?.addPolyline(
                PolylineOptions()
                    .addAll(rutaPendiente)
                    .color(android.graphics.Color.parseColor("#1565C0"))
                    .width(8f)
                    .geodesic(true)
            )
        }

        val recycler = findViewById<RecyclerView>(R.id.rvParadas)
        if (indiceMasCercano != indiceParadaActual) {
            indiceParadaActual = indiceMasCercano
            (recycler.adapter as? ParadasAdapter)?.actualizarProgreso(indiceMasCercano)
        }


    }

    private fun distancia(a: LatLng, b: LatLng): Double {
        val dLat = a.latitude - b.latitude
        val dLng = a.longitude - b.longitude
        return dLat * dLat + dLng * dLng
    }

    override fun onDestroy() {
        super.onDestroy()
        socket?.disconnect()
    }



    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


}




