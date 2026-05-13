package mx.itson.potrobus.utils

import com.google.gson.GsonBuilder
import mx.itson.potrobus.interfaces.PotrobusAPI
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitUtil {

        fun getApiPotrobusAPI() : PotrobusAPI {
            val gson = GsonBuilder().create()
            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            return retrofit.create(PotrobusAPI::class.java)
        }

}