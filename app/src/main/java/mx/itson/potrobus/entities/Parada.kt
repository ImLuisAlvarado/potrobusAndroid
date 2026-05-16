package mx.itson.potrobus.entities

import android.util.Log
import mx.itson.potrobus.utils.RetrofitUtil

class Parada {
    constructor()
    constructor(id_parada: Int, id_ruta: Int, nombre: String, latitud: Double, longitud: Double, orden: Int) {
        this.id_parada = id_parada
        this.id_ruta = id_ruta
        this.nombre = nombre
        this.latitud = latitud
        this.longitud = longitud
        this.orden = orden
    }

    var id_parada: Int? = null
    var id_ruta: Int? = null
    var nombre: String? = null
    var latitud: Double? = null
    var longitud: Double? = null
    var orden: Int? = null

    fun getByRuta(token: String, idUnidad: Int, callback: (List<Parada>?) -> Unit) {
        val call = RetrofitUtil.getApiPotrobusAPI().getRutaByUnidad("Bearer $token", idUnidad)
        call.enqueue(object : retrofit2.Callback<RutaConParadas> {
            override fun onResponse(
                call: retrofit2.Call<RutaConParadas>,
                response: retrofit2.Response<RutaConParadas>
            ) {
                Log.d("PARADAS", "Código HTTP: ${response.code()}")
                Log.d("PARADAS", "Body completo: ${response.body()}")
                Log.d("PARADAS", "Paradas: ${response.body()?.paradas}")
                Log.d("PARADAS", "Error body: ${response.errorBody()?.string()}")
                callback(response.body()?.paradas)
            }

            override fun onFailure(call: retrofit2.Call<RutaConParadas>, t: Throwable) {
                Log.e("PARADAS", "Error: ${t.message}")
                callback(null)
            }
        })
    }
}