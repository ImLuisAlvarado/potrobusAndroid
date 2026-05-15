package mx.itson.potrobus.interfaces

import mx.itson.potrobus.entities.LoginRequest
import mx.itson.potrobus.entities.LoginResponse
import mx.itson.potrobus.entities.Parada
import mx.itson.potrobus.entities.RegisterRequest
import mx.itson.potrobus.entities.RegisterResponse
import mx.itson.potrobus.entities.Ruta
import mx.itson.potrobus.entities.Unidad
import mx.itson.potrobus.entities.RutaConParadas
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PotrobusAPI {

    @GET("api/rutas")
    fun getRutas(@Header("Authorization") token: String): Call<List<Ruta>>

    @GET("api/rutas/{id_ruta}/paradas")
    fun getParadas(
        @Header("Authorization") token: String,
        @Path("id_ruta") idRuta: Int
    ): Call<List<Parada>>

    @POST("login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    @POST("api/register")
    fun register(@Body body: RegisterRequest): Call<RegisterResponse>

    @GET("api/buses/activos")
    fun getBusesActivos(@Header("Authorization") token: String): Call<List<Unidad>>


    @GET("api/buses/{id_unidad}/ruta")
    fun getRutaByUnidad(
        @Header("Authorization") token: String,
        @Path("id_unidad") idUnidad: Int
    ): Call<RutaConParadas>
}