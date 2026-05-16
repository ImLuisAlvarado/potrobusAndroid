package mx.itson.potrobus

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.socket.client.IO
import io.socket.client.Socket
import mx.itson.potrobus.adapters.ParadasAdapter
import mx.itson.potrobus.entities.Parada
import mx.itson.potrobus.utils.Constants
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Intent


class MapViewActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var map: MapLibreMap? = null
    private var socket: Socket? = null

    private var symbolManager: SymbolManager? = null
    private var lineManager: LineManager? = null
    private var busSymbol: org.maplibre.android.plugins.annotation.Symbol? = null

    private val paradaCoords = mutableListOf<LatLng>()
    private val rutaCompleta = mutableListOf<LatLng>()
    private var indiceParadaActual = -1

    private val BASE_URL = Constants.BASE_URL.dropLast(1)
    private val GUAYMAS_CENTER = LatLng(27.9600, -110.8600)
    private var idUnidadSeleccionada = 1

    private var lastGpsTime = 0L
    private val GPS_TIMEOUT_MS = 30_000L
    private var signalLost = false

    // URL de tiles OpenStreetMap — sin API key
    private val OSM_STYLE = "https://tiles.openfreemap.org/styles/liberty"

    companion object {
        const val BUS_ICON_ID = "bus-icon"
        const val PARADA_ICON_ID = "parada-icon"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar MapLibre (reemplaza Google Maps — sin API key)
        MapLibre.getInstance(this)

        setContentView(R.layout.activity_map_view)

        idUnidadSeleccionada = intent.getIntExtra("id_unidad", 1)
        val numeroEconomico = intent.getStringExtra("numero_economico") ?: "PotroBus"
        findViewById<TextView>(R.id.tvParadasTitle).text = "Ruta — $numeroEconomico"

        mapView = findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        if (!isOnline()) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        connectSocket()
    }

    override fun onMapReady(mapLibreMap: MapLibreMap) {
        map = mapLibreMap

        // Cargar estilo OpenStreetMap
        mapLibreMap.setStyle(Style.Builder().fromUri(OSM_STYLE)) { style ->

            // Registrar iconos
            scaledBitmap(R.drawable.ic_bus, 48)?.let {
                style.addImage(BUS_ICON_ID, it)
            }
            // Círculo azul pequeño para paradas (usa ic_bus si no tienes otro drawable)
            scaledBitmap(R.drawable.ic_bus, 24)?.let {
                style.addImage(PARADA_ICON_ID, it)
            }

            symbolManager = SymbolManager(mapView, mapLibreMap, style).apply {
                iconAllowOverlap = true
                textAllowOverlap = true
            }
            lineManager = LineManager(mapView, mapLibreMap, style)

            // Cámara inicial en Guaymas
            mapLibreMap.cameraPosition = CameraPosition.Builder()
                .target(GUAYMAS_CENTER)
                .zoom(13.0)
                .build()

            // Cargar paradas desde el backend
            val token = getSharedPreferences("potrobus_prefs", MODE_PRIVATE)
                .getString("jwt_token", "") ?: ""

            Log.d("TOKEN_DEBUG", "Token: '$token'")  // ← agrega esto
            Log.d("TOKEN_DEBUG", "Header: 'Bearer $token'")
            Parada().getByRuta(token, idUnidadSeleccionada) { paradas ->
                if (paradas == null) {
                    // ❌ Antes cerraba sesión — incorrecto
                    // Ahora solo muestra error sin cerrar sesión
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "No se pudo cargar la ruta, intenta de nuevo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@getByRuta
                }

                if (paradas.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Esta unidad no tiene paradas asignadas",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@getByRuta
                }

                runOnUiThread {
                    paradas.forEach { parada ->
                        val lat = parada.latitud ?: return@forEach
                        val lng = parada.longitud ?: return@forEach
                        val pos = LatLng(lat, lng)
                        paradaCoords.add(pos)

                        symbolManager?.create(
                            SymbolOptions()
                                .withLatLng(pos)
                                .withIconImage(PARADA_ICON_ID)
                                .withTextField(parada.nombre ?: "")
                                .withTextOffset(arrayOf(0f, 1.5f))
                                .withTextSize(11f)
                        )
                    }

                    rutaCompleta.clear()
                    rutaCompleta.addAll(paradaCoords)
                    dibujarRuta(rutaCompleta)
                    setupParadasList(paradas)
                }
            }
        }
    }

    private fun dibujarRuta(coords: List<LatLng>) {
        if (coords.size < 2) return
        lineManager?.deleteAll()
        lineManager?.create(
            LineOptions()
                .withLatLngs(coords)
                .withLineColor("#1565C0")
                .withLineWidth(4f)
        )
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
                        Toast.makeText(
                            this@MapViewActivity,
                            "Sesión expirada, inicia sesión de nuevo",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }

            socket?.on("gps_live") { args ->
                try {
                    val data = args[0] as JSONObject
                    val idUnidad = data.optInt("id_unidad", -1)

                    // ← Solo procesar si es el autobús que seleccionamos
                    if (idUnidad != idUnidadSeleccionada) return@on

                    lastGpsTime = System.currentTimeMillis()
                    runOnUiThread {
                        if (signalLost) {
                            signalLost = false
                            Toast.makeText(
                                this@MapViewActivity,
                                "Señal GPS recuperada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
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

            // Watchdog para señal GPS perdida
            val handler = android.os.Handler(mainLooper)
            val checkGps = object : Runnable {
                override fun run() {
                    if (lastGpsTime > 0 && System.currentTimeMillis() - lastGpsTime > GPS_TIMEOUT_MS) {
                        if (!signalLost) {
                            signalLost = true
                            Toast.makeText(
                                this@MapViewActivity,
                                "Señal GPS perdida",
                                Toast.LENGTH_SHORT
                            ).show()
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
        if (busSymbol == null) {
            busSymbol = symbolManager?.create(
                SymbolOptions()
                    .withLatLng(pos)
                    .withIconImage(BUS_ICON_ID)
                    .withIconSize(1.5f)
            )
            map?.cameraPosition = CameraPosition.Builder()
                .target(pos)
                .zoom(14.0)
                .build()
        } else {
            busSymbol?.latLng = pos
            symbolManager?.update(busSymbol)
        }

        if (rutaCompleta.isEmpty()) return

        // Encontrar punto más cercano en la ruta
        val indiceMasCercano = rutaCompleta.indices.minByOrNull { i ->
            distancia(pos, rutaCompleta[i])
        } ?: return

        val distanciaAlPunto =
            kotlin.math.sqrt(distancia(pos, rutaCompleta[indiceMasCercano])) * 111000
        if (distanciaAlPunto > 500) return

        // Redibujar solo la porción pendiente
        val rutaPendiente = rutaCompleta.subList(indiceMasCercano, rutaCompleta.size)
        if (rutaPendiente.size >= 2) {
            dibujarRuta(rutaPendiente)
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

    private fun scaledBitmap(resId: Int, sizeDp: Int): Bitmap? {
        val density = resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        val original = BitmapFactory.decodeResource(resources, resId) ?: return null
        return Bitmap.createScaledBitmap(original, sizePx, sizePx, false)
    }

    // ── Ciclo de vida del MapView (obligatorio con MapLibre) ──────────────────
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        super.onDestroy()
        socket?.disconnect()
        mapView.onDestroy()
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}