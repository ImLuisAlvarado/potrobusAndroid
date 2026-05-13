package mx.itson.potrobus.entities

import mx.itson.potrobus.utils.RetrofitUtil

class Ruta {
        constructor()
        constructor(id_ruta: Int, nombre: String, origen: String, destino: String, descripcion: String?) {
            this.id_ruta = id_ruta
            this.nombre = nombre
            this.origen = origen
            this.destino = destino
            this.descripcion = descripcion
        }

        var id_ruta: Int? = null
        var nombre: String? = null
        var origen: String? = null
        var destino: String? = null
        var descripcion: String? = null

        fun getAll(token: String, callback: (List<Ruta>?) -> Unit) {
            val call = RetrofitUtil.getApiPotrobusAPI().getRutas("Bearer $token")
            call.enqueue(object : retrofit2.Callback<List<Ruta>> {
                override fun onResponse(call: retrofit2.Call<List<Ruta>>, response: retrofit2.Response<List<Ruta>>) {
                    callback(response.body())
                }
                override fun onFailure(call: retrofit2.Call<List<Ruta>>, t: Throwable) {
                    callback(null)
                }
            })
        }
}