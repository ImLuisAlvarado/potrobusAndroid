package mx.itson.potrobus.utils

import com.google.gson.GsonBuilder
import mx.itson.potrobus.interfaces.PotrobusAPI
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitUtil {

    private val api: PotrobusAPI by lazy {
        val gson = GsonBuilder().create()
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(PotrobusAPI::class.java)
    }

        fun getApiPotrobusAPI() : PotrobusAPI = api

}