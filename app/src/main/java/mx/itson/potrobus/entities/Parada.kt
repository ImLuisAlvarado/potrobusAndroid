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

    fun getByRuta(token: String, idRuta: Int, callback: (List<Parada>?) -> Unit) {
        val call = RetrofitUtil.getApiPotrobusAPI().getParadas("Bearer $token", idRuta)
        call.enqueue(object : retrofit2.Callback<List<Parada>> {
            override fun onResponse(call: retrofit2.Call<List<Parada>>, response: retrofit2.Response<List<Parada>>) {
                Log.d("PARADAS", "Código HTTP: ${response.code()}")
                Log.d("PARADAS", "Body: ${response.body()}")
                Log.d("PARADAS", "Error body: ${response.errorBody()?.string()}")
                callback(response.body())
            }
            override fun onFailure(call: retrofit2.Call<List<Parada>>, t: Throwable) {
                Log.e("PARADAS", "Error: ${t.message}")
                callback(null)
            }
        })
    }
}